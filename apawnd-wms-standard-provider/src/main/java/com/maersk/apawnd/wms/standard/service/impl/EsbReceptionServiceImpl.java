package com.maersk.apawnd.wms.standard.service.impl;

import com.maersk.apawnd.wms.standard.dto.*;
import com.maersk.apawnd.wms.standard.mapper.EsbRcptMainMapper;
import com.maersk.apawnd.wms.standard.mapper.EsbRcptMapper;
import com.maersk.apawnd.wms.standard.mapper.EsbRcptMsgPreambleMapper;
import com.maersk.apawnd.wms.standard.mapper.EsbRcptSerialNoMapper;
import com.maersk.apawnd.wms.standard.model.EsbRcptMainModel;
import com.maersk.apawnd.wms.standard.model.EsbRcptModel;
import com.maersk.apawnd.wms.standard.model.EsbRcptMsgPreambleModel;
import com.maersk.apawnd.wms.standard.model.EsbRcptSerialNoModel;
import com.maersk.apawnd.wms.standard.service.EsbReceptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class EsbReceptionServiceImpl implements EsbReceptionService {

  private final EsbRcptMapper esbRcptMapper;
  private final EsbRcptMainMapper esbRcptMainMapper;
  private final EsbRcptMsgPreambleMapper esbRcptMsgPreambleMapper;
  private final EsbRcptSerialNoMapper esbRcptSerialNoMapper;

  public EsbReceptionServiceImpl(
      EsbRcptMapper esbRcptMapper,
      EsbRcptMainMapper esbRcptMainMapper,
      EsbRcptMsgPreambleMapper esbRcptMsgPreambleMapper,
      EsbRcptSerialNoMapper esbRcptSerialNoMapper) {
    this.esbRcptMapper = esbRcptMapper;
    this.esbRcptMainMapper = esbRcptMainMapper;
    this.esbRcptMsgPreambleMapper = esbRcptMsgPreambleMapper;
    this.esbRcptSerialNoMapper = esbRcptSerialNoMapper;
  }

  @Override
  @Transactional
  public GrnAckDto generateGrnAck(String eventData) {
    GrnAckDto grnAckDto = new GrnAckDto();
    EsbRcptDto esbRcptDto = composeEsbRcptDto(eventData);
    grnAckDto.setEsbRcptDto(esbRcptDto);
    esbRcptDto.setEsbRcptMsgPreambleCollectionDto(composeEsbRcptMsgPreambleDto(eventData));
    esbRcptDto.setEsbRcptMainCollectionDto(composeEsbRcptMainDto(eventData));

    return grnAckDto;
  }

  private EsbRcptDto composeEsbRcptDto(String eventData) {
    EsbRcptDto esbRcptDto = new EsbRcptDto();
    List<EsbRcptModel> esbRcptModelList = esbRcptMapper.selectByHjsNodeId(eventData);
    if (Objects.nonNull(esbRcptModelList) && esbRcptModelList.size() > 0) {
      BeanUtils.copyProperties(esbRcptModelList.get(0), esbRcptDto);
    }
    return esbRcptDto;
  }

  private EsbRcptMsgPreambleCollectionDto composeEsbRcptMsgPreambleDto(String eventData) {
    EsbRcptMsgPreambleCollectionDto esbRcptMsgPreambleCollectionDto =
        new EsbRcptMsgPreambleCollectionDto();
    List<EsbRcptMsgPreambleModel> esbRcptMsgPreambleModelList =
        esbRcptMsgPreambleMapper.selectByHjsParentId(eventData);
    if (Objects.nonNull(esbRcptMsgPreambleModelList) && esbRcptMsgPreambleModelList.size() > 0) {
      EsbRcptMsgPreambleDto esbRcptMsgPreambleDto = new EsbRcptMsgPreambleDto();
      BeanUtils.copyProperties(esbRcptMsgPreambleModelList.get(0), esbRcptMsgPreambleDto);
      esbRcptMsgPreambleCollectionDto.setEsbRcptMsgPreambleDto(esbRcptMsgPreambleDto);
    }
    return esbRcptMsgPreambleCollectionDto;
  }

  private EsbRcptMainCollectionDto composeEsbRcptMainDto(String eventData) {
    EsbRcptMainCollectionDto esbRcptMainCollectionDto = new EsbRcptMainCollectionDto();
    List<EsbRcptMainDto> esbRcptMainDtoList = new ArrayList<>();
    esbRcptMainCollectionDto.setEsbRcptMainDtoList(esbRcptMainDtoList);

    List<EsbRcptMainModel> esbRcptMainModelList = esbRcptMainMapper.selectByHjsParentId(eventData);
    List<EsbRcptSerialNoModel> esbRcptSerialNoModelList = esbRcptSerialNoMapper.selectByHjsParentId(eventData);

    if (Objects.nonNull(esbRcptMainModelList) && esbRcptMainModelList.size() > 0) {
      esbRcptMainModelList.stream().forEach(esbRcptMainModel -> {
        EsbRcptMainDto esbRcptMainDto = new EsbRcptMainDto();
        BeanUtils.copyProperties(esbRcptMainModel, esbRcptMainDto);
        esbRcptMainDtoList.add(esbRcptMainDto);

        if (Objects.nonNull(esbRcptSerialNoModelList) && esbRcptSerialNoModelList.size() > 0) {
          EsbRcptSerialNoCollectionDto esbRcptSerialNoCollectionDto = new EsbRcptSerialNoCollectionDto();
          List<EsbRcptSerialNoDto> esbRcptSerialNoDtoList = new ArrayList<>();
          esbRcptSerialNoCollectionDto.setEsbRcptSerialNoDtoList(esbRcptSerialNoDtoList);
          esbRcptSerialNoModelList.stream().filter(
              esbRcptSerialNoModel -> esbRcptMainDto.getHjsNodeId().equals(esbRcptSerialNoModel.getHjsParentId())
          ).forEach(esbRcptSerialNoModel -> {
            EsbRcptSerialNoDto esbRcptSerialNoDto = new EsbRcptSerialNoDto();
            BeanUtils.copyProperties(esbRcptSerialNoModel, esbRcptSerialNoDto);
            esbRcptSerialNoDtoList.add(esbRcptSerialNoDto);
          });
          esbRcptMainDto.setEsbRcptSerialNoCollectionDto(esbRcptSerialNoCollectionDto);
        }
      });
    }

    return esbRcptMainCollectionDto;
  }
}
