package com.maersk.highjump.generics.service.impl;

import com.maersk.commons.component.exception.BusinessException;
import com.maersk.commons.component.util.ErrorUtil;
import com.maersk.highjump.generics.component.constant.MessageConstant;
import com.maersk.highjump.generics.dto.AsnDto;
import com.maersk.highjump.generics.dto.RcptShipPoDetailCartonDto;
import com.maersk.highjump.generics.dto.RcptShipPoDetailDto;
import com.maersk.highjump.generics.dto.RcptShipPoDto;
import com.maersk.highjump.generics.component.enums.*;
import com.maersk.highjump.generics.mapper.*;
import com.maersk.highjump.generics.model.*;
import com.maersk.highjump.generics.service.ShipmentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ShipmentServiceImpl implements ShipmentService {

  private final RcptShipMapper rcptShipMapper;
  private final RcptShipPoMapper rcptShipPoMapper;
  private final RcptShipPoDetailMapper rcptShipPoDetailMapper;
  private final RcptShipCartonDetailMapper rcptShipCartonDetailMapper;
  private final ReceiptMapper receiptMapper;
  private final CarrierMapper carrierMapper;
  private final ErrorUtil errorUtil;

  public ShipmentServiceImpl(
      RcptShipMapper rcptShipMapper,
      RcptShipPoMapper rcptShipPoMapper,
      RcptShipPoDetailMapper rcptShipPoDetailMapper,
      RcptShipCartonDetailMapper rcptShipCartonDetailMapper,
      ReceiptMapper receiptMapper,
      CarrierMapper carrierMapper,
      ErrorUtil errorUtil) {
    this.rcptShipMapper = rcptShipMapper;
    this.rcptShipPoMapper = rcptShipPoMapper;
    this.rcptShipPoDetailMapper = rcptShipPoDetailMapper;
    this.rcptShipCartonDetailMapper = rcptShipCartonDetailMapper;
    this.receiptMapper = receiptMapper;
    this.carrierMapper = carrierMapper;
    this.errorUtil = errorUtil;
  }

  @Override
  @Transactional
  public void createAsn(AsnDto asnDto) {
    deleteExistedAsn(asnDto);
    insertAsn(asnDto);
  }

  private void deleteExistedAsn(AsnDto asnDto) {
    List<RcptShipModel> rcptShipModels =
        rcptShipMapper.selectByWhIdAndShipmentNumber(
            asnDto.getWhId(),
            asnDto.getShipmentNumber());

    if (Objects.isNull(rcptShipModels) || rcptShipModels.size() == 0) {
      throw new BusinessException(
          errorUtil.build400ErrorList(MessageConstant.MESSAGE_KEY_E01_0004));
    }

    String asnStatus = rcptShipModels.get(0).getStatus();
    if (ShipmentStatusEnum.SHIPMENT_STATUS_CODE_CLOSED.getCode().equals(asnStatus)) {
      throw new BusinessException(
          errorUtil.build400ErrorList(MessageConstant.MESSAGE_KEY_E01_0005));
    }

    if (ShipmentStatusEnum.SHIPMENT_STATUS_CODE_RECONCILED.getCode().equals(asnStatus)) {
      throw new BusinessException(
          errorUtil.build400ErrorList(MessageConstant.MESSAGE_KEY_E01_0006));
    }

    int count =
        receiptMapper.selectCountByWhIdAndShipmentNumberAndStatus(
            asnDto.getWhId(),
            asnDto.getShipmentNumber(),
            ReceiptStatusEnum.RECEIPT_STATUS_CODE_OPEN.getCode());
    if (count == 0) {
      throw new BusinessException(
          errorUtil.build400ErrorList(MessageConstant.MESSAGE_KEY_E01_0007));
    }

    rcptShipMapper.deleteByWhIdAndShipmentNumber(asnDto.getWhId(), asnDto.getShipmentNumber());
    rcptShipPoMapper.deleteByWhIdAndShipmentNumber(asnDto.getWhId(), asnDto.getShipmentNumber());
    rcptShipPoDetailMapper.deleteByWhIdAndShipmentNumber(asnDto.getWhId(), asnDto.getShipmentNumber());
    rcptShipCartonDetailMapper.deleteByWhIdAndShipmentNumber(asnDto.getWhId(), asnDto.getShipmentNumber());
  }

  private void insertAsn(AsnDto asnDto) {
    RcptShipModel rcptShipModel =
        composeRcptShipPoModel(asnDto);
    rcptShipMapper.insert(rcptShipModel);

    List<RcptShipPoModel> rcptShipPoModelList = new ArrayList<>();
    List<RcptShipPoDetailModel> rcptShipPoDetailModelList = new ArrayList<>();
    List<RcptShipCartonDetailModel> rcptShipCartonDetailModelList = new ArrayList<>();

    if (Objects.nonNull(asnDto.getPoList()) &&
        asnDto.getPoList().size() > 0) {
      asnDto.getPoList().stream().forEach(
          rcptShipPoDto -> {
            RcptShipPoModel rcptShipPoModel =
                composeRcptShipPoModel(asnDto, rcptShipPoDto);
            rcptShipPoModelList.add(rcptShipPoModel);

            if (Objects.nonNull(rcptShipPoDto.getDetailList()) &&
                rcptShipPoDto.getDetailList().size() > 0) {
              rcptShipPoDto.getDetailList().stream().forEach(rcptShipPoDetailDto -> {
                RcptShipPoDetailModel rcptShipPoDetailModel =
                    composeRcptShipPoDetailModel(asnDto, rcptShipPoDto, rcptShipPoDetailDto);
                rcptShipPoDetailModelList.add(rcptShipPoDetailModel);

                if (Objects.nonNull(rcptShipPoDetailDto.getCartonList()) &&
                    rcptShipPoDetailDto.getCartonList().size() > 0) {
                  rcptShipPoDetailDto.getCartonList().stream().forEach(rcptShipPoDetailCartonDto -> {
                    RcptShipCartonDetailModel rcptShipCartonDetailModel =
                        composeRcptShipCartonDetailModel(asnDto, rcptShipPoDto, rcptShipPoDetailDto, rcptShipPoDetailCartonDto);
                    rcptShipCartonDetailModelList.add(rcptShipCartonDetailModel);
                  });
                }
              });
            }
          });
      rcptShipPoMapper.insertBatch(rcptShipPoModelList);
      rcptShipPoDetailMapper.insertBatch(rcptShipPoDetailModelList);
      rcptShipCartonDetailMapper.insertBatch(rcptShipCartonDetailModelList);
    }
  }

  private RcptShipModel composeRcptShipPoModel(AsnDto asnDto) {
    RcptShipModel rcptShipModel = new RcptShipModel();
    rcptShipModel.setWhId(asnDto.getWhId());
    rcptShipModel.setShipmentNumber(asnDto.getShipmentNumber());
    List<CarrierModel> carrierModels =
        carrierMapper.selectByCarrierName(asnDto.getCarrierName());
    if (Objects.isNull(carrierModels) || carrierModels.size() == 0) {
      throw new BusinessException(
          errorUtil.build400ErrorList(MessageConstant.MESSAGE_KEY_E01_0008));
    }
    rcptShipModel.setCarrierId(carrierModels.get(0).getCarrierId());
    rcptShipModel.setTrailerNumber(asnDto.getTrailerNumber());
    if (Objects.isNull(asnDto.getDateExpected())) {
      asnDto.setDateExpected(Instant.now());
    }
    rcptShipModel.setDateExpected(asnDto.getDateExpected());
    if (Objects.isNull(asnDto.getDateReceived())) {
      asnDto.setDateReceived(Instant.now());
    }
    rcptShipModel.setDateReceived(asnDto.getDateReceived());
    if (Objects.isNull(asnDto.getDateShipped())) {
      asnDto.setDateShipped(Instant.now());
    }
    rcptShipModel.setDateShipped(asnDto.getDateShipped());
    if (StringUtils.isEmpty(asnDto.getStatus())) {
      asnDto.setStatus(ShipmentStatusEnum.SHIPMENT_STATUS_CODE_OPEN.getCode());
    }
    rcptShipModel.setStatus(asnDto.getStatus());
    rcptShipModel.setComments(asnDto.getComments());
    rcptShipModel.setWorkersAssigned(asnDto.getWorkersAssigned());
    rcptShipModel.setProNumber(asnDto.getProNumber());
    rcptShipModel.setArrivalSign(asnDto.getArrivalSign());
    rcptShipModel.setArrivalDate(asnDto.getArrivalDate());
    rcptShipModel.setGrnSendSign(asnDto.getGrnSendSign());
    rcptShipModel.setGrnSendDate(asnDto.getGrnSendDate());
    rcptShipModel.setShipmentReceiptDate(asnDto.getShipmentReceiptDate());
    rcptShipModel.setAsnType(asnDto.getAsnType());
    rcptShipModel.setWipNumber(asnDto.getWipNumber());
    rcptShipModel.setFileSeq(asnDto.getFileSeq());
    rcptShipModel.setTallysheetIsPrinted(asnDto.getTallysheetIsPrinted());

    return rcptShipModel;
  }

  private RcptShipPoModel composeRcptShipPoModel(
      AsnDto asnDto,
      RcptShipPoDto rcptShipPoDto) {
    RcptShipPoModel rcptShipPoModel = new RcptShipPoModel();
    rcptShipPoModel.setWhId(asnDto.getWhId());
    rcptShipPoModel.setShipmentNumber(asnDto.getShipmentNumber());
    rcptShipPoModel.setPoNumber(rcptShipPoDto.getPoNumber());
    rcptShipPoModel.setCasesExpected(rcptShipPoDto.getCasesExpected());
    rcptShipPoModel.setCasesReceived(rcptShipPoDto.getCasesReceived());
    rcptShipPoModel.setOpenToBuyDate(rcptShipPoDto.getOpenToBuyDate());

    return rcptShipPoModel;
  }

  private RcptShipPoDetailModel composeRcptShipPoDetailModel(
      AsnDto asnDto,
      RcptShipPoDto rcptShipPoDto,
      RcptShipPoDetailDto rcptShipPoDetailDto) {
    RcptShipPoDetailModel rcptShipPoDetailModel = new RcptShipPoDetailModel();
    rcptShipPoDetailModel.setWhId(asnDto.getWhId());
    rcptShipPoDetailModel.setShipmentNumber(asnDto.getShipmentNumber());
    rcptShipPoDetailModel.setPoNumber(rcptShipPoDto.getPoNumber());
    rcptShipPoDetailModel.setLineNumber(rcptShipPoDetailDto.getLineNumber());
    rcptShipPoDetailModel.setItemNumber(rcptShipPoDetailDto.getItemNumber());
    if (Objects.isNull(rcptShipPoDetailDto.getScheduleNumber())) {
      rcptShipPoDetailDto.setScheduleNumber(1);
    }
    rcptShipPoDetailModel.setScheduleNumber(rcptShipPoDetailDto.getScheduleNumber());
    rcptShipPoDetailModel.setExpectedQty(rcptShipPoDetailDto.getExpectedQty());
    rcptShipPoDetailModel.setReceivedQty(rcptShipPoDetailDto.getReceivedQty());
    rcptShipPoDetailModel.setReconciledDate(rcptShipPoDetailDto.getReconciledDate());
    rcptShipPoDetailModel.setStatus(rcptShipPoDetailDto.getStatus());
    rcptShipPoDetailModel.setShipmentLineNumber(rcptShipPoDetailDto.getShipmentLineNumber());
    rcptShipPoDetailModel.setWeight(rcptShipPoDetailDto.getWeight());
    rcptShipPoDetailModel.setCalculationUom(rcptShipPoDetailDto.getCalculationUom());
    rcptShipPoDetailModel.setPropertyMark(rcptShipPoDetailDto.getPropertyMark());
    rcptShipPoDetailModel.setGenericField1(rcptShipPoDetailDto.getGenericField1());
    rcptShipPoDetailModel.setGenericField2(rcptShipPoDetailDto.getGenericField2());
    rcptShipPoDetailModel.setGenericField3(rcptShipPoDetailDto.getGenericField3());
    rcptShipPoDetailModel.setGenericField4(rcptShipPoDetailDto.getGenericField4());
    rcptShipPoDetailModel.setGenericField5(rcptShipPoDetailDto.getGenericField5());
    rcptShipPoDetailModel.setGenericField6(rcptShipPoDetailDto.getGenericField6());
    rcptShipPoDetailModel.setGenericField7(rcptShipPoDetailDto.getGenericField7());
    rcptShipPoDetailModel.setGenericField8(rcptShipPoDetailDto.getGenericField8());
    rcptShipPoDetailModel.setGenericField9(rcptShipPoDetailDto.getGenericField9());
    rcptShipPoDetailModel.setGenericField10(rcptShipPoDetailDto.getGenericField10());
    rcptShipPoDetailModel.setGenericField11(rcptShipPoDetailDto.getGenericField11());
    rcptShipPoDetailModel.setGenericField12(rcptShipPoDetailDto.getGenericField12());
    rcptShipPoDetailModel.setGenericField13(rcptShipPoDetailDto.getGenericField13());
    rcptShipPoDetailModel.setGenericField14(rcptShipPoDetailDto.getGenericField14());
    rcptShipPoDetailModel.setGenericField15(rcptShipPoDetailDto.getGenericField15());
    rcptShipPoDetailModel.setGrnSendSign(rcptShipPoDetailDto.getGrnSendSign());
    rcptShipPoDetailModel.setGrnSendDate(rcptShipPoDetailDto.getGrnSendDate());
    rcptShipPoDetailModel.setCustomsIsApproved(rcptShipPoDetailDto.getCustomsIsApproved());
    rcptShipPoDetailModel.setBondedQty(rcptShipPoDetailDto.getBondedQty());
    rcptShipPoDetailModel.setFreeQty(rcptShipPoDetailDto.getFreeQty());
    rcptShipPoDetailModel.setConfirmBounded(rcptShipPoDetailDto.getConfirmBounded());
    rcptShipPoDetailModel.setConfirmFree(rcptShipPoDetailDto.getConfirmFree());
    rcptShipPoDetailModel.setConfirmKit(rcptShipPoDetailDto.getConfirmKit());
    rcptShipPoDetailModel.setReceivedBondedQty(rcptShipPoDetailDto.getReceivedBondedQty());
    rcptShipPoDetailModel.setReceivedFreeQty(rcptShipPoDetailDto.getReceivedFreeQty());

    return rcptShipPoDetailModel;
  }

  private RcptShipCartonDetailModel composeRcptShipCartonDetailModel(
      AsnDto asnDto,
      RcptShipPoDto rcptShipPoDto,
      RcptShipPoDetailDto rcptShipPoDetailDto,
      RcptShipPoDetailCartonDto rcptShipPoDetailCartonDto) {
    RcptShipCartonDetailModel rcptShipCartonDetailModel = new RcptShipCartonDetailModel();
    rcptShipCartonDetailModel.setWhId(asnDto.getWhId());
    rcptShipCartonDetailModel.setShipmentNumber(asnDto.getShipmentNumber());
    rcptShipCartonDetailModel.setPoNumber(rcptShipPoDto.getPoNumber());
    rcptShipCartonDetailModel.setLineNumber(rcptShipPoDetailDto.getLineNumber());
    rcptShipCartonDetailModel.setItemNumber(rcptShipPoDetailDto.getItemNumber());
    rcptShipCartonDetailModel.setScheduleNumber(rcptShipPoDetailDto.getScheduleNumber());
    rcptShipCartonDetailModel.setExpectQty(rcptShipPoDetailCartonDto.getExpectQty());
    rcptShipCartonDetailModel.setUcc(rcptShipPoDetailCartonDto.getUcc());
    rcptShipCartonDetailModel.setReference1(rcptShipPoDetailCartonDto.getReference1());
    rcptShipCartonDetailModel.setReference2(rcptShipPoDetailCartonDto.getReference2());
    rcptShipCartonDetailModel.setReference3(rcptShipPoDetailCartonDto.getReference3());
    rcptShipCartonDetailModel.setReference4(rcptShipPoDetailCartonDto.getReference4());
    rcptShipCartonDetailModel.setReference5(rcptShipPoDetailCartonDto.getReference5());
    rcptShipCartonDetailModel.setStatus(rcptShipPoDetailCartonDto.getStatus());
    rcptShipCartonDetailModel.setForkId(rcptShipPoDetailCartonDto.getForkId());
    rcptShipCartonDetailModel.setDimension(rcptShipPoDetailCartonDto.getDimension());
    rcptShipCartonDetailModel.setHuId(rcptShipPoDetailCartonDto.getHuId());
    rcptShipCartonDetailModel.setGenericField1(rcptShipPoDetailCartonDto.getGenericField1());
    rcptShipCartonDetailModel.setGenericField2(rcptShipPoDetailCartonDto.getGenericField2());
    rcptShipCartonDetailModel.setGenericField3(rcptShipPoDetailCartonDto.getGenericField3());
    rcptShipCartonDetailModel.setGenericField4(rcptShipPoDetailCartonDto.getGenericField4());
    rcptShipCartonDetailModel.setGenericField5(rcptShipPoDetailCartonDto.getGenericField5());
    rcptShipCartonDetailModel.setGenericField6(rcptShipPoDetailCartonDto.getGenericField6());
    rcptShipCartonDetailModel.setGenericField7(rcptShipPoDetailCartonDto.getGenericField7());
    rcptShipCartonDetailModel.setGenericField8(rcptShipPoDetailCartonDto.getGenericField8());
    rcptShipCartonDetailModel.setGenericField9(rcptShipPoDetailCartonDto.getGenericField9());
    rcptShipCartonDetailModel.setGenericField10(rcptShipPoDetailCartonDto.getGenericField10());
    rcptShipCartonDetailModel.setGenericField11(rcptShipPoDetailCartonDto.getGenericField11());
    rcptShipCartonDetailModel.setGenericField12(rcptShipPoDetailCartonDto.getGenericField12());
    rcptShipCartonDetailModel.setGenericField13(rcptShipPoDetailCartonDto.getGenericField13());
    rcptShipCartonDetailModel.setGenericField14(rcptShipPoDetailCartonDto.getGenericField14());
    rcptShipCartonDetailModel.setGenericField15(rcptShipPoDetailCartonDto.getGenericField15());
    rcptShipCartonDetailModel.setSendToLocalDb(rcptShipPoDetailCartonDto.getSendToLocalDb());
    rcptShipCartonDetailModel.setSendDate(rcptShipPoDetailCartonDto.getSendDate());
    rcptShipCartonDetailModel.setContainerLabel(rcptShipPoDetailCartonDto.getContainerLabel());
    rcptShipCartonDetailModel.setPickingListId(rcptShipPoDetailCartonDto.getPickingListId());
    rcptShipCartonDetailModel.setScanFlag(rcptShipPoDetailCartonDto.getScanFlag());
    rcptShipCartonDetailModel.setQtyReceived(rcptShipPoDetailCartonDto.getQtyReceived());

    return rcptShipCartonDetailModel;
  }
}