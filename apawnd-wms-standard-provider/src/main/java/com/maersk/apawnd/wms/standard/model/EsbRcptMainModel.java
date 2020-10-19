package com.maersk.apawnd.wms.standard.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.Alias;

import java.io.Serializable;

@Data
@NoArgsConstructor
@Alias("EsbRcptMainModel")
public class EsbRcptMainModel implements Serializable {

  private String hjsParentId;
  private String hjsNodeId;
  private Integer hjsSequence;
  private Integer hjsErrorNumber;
  private String hjsErrorMessage;
  private String receiptId;
  private String messageId;
  private String poNumber;
  private String displayPoNumber;
  private String receiptDate;
  private String carrierScacCode;
  private String status;
  private String itemNumber;
  private String displayItemNumber;
  private String lotNumber;
  private String lineNumber;
  private String scheduleNumber;
  private String qtyReceived;
  private String qtyDamaged;
  private String huId;
  private String packingSlipNo;
  private String forkId;
  private String uom;
  private String shipmentNumber;
  private String whId;
  private String clientCode;
  private String genericAttribute1;
  private String genericAttribute2;
  private String genericAttribute3;
  private String genericAttribute4;
  private String genericAttribute5;
  private String genericAttribute6;
  private String genericAttribute7;
  private String genericAttribute8;
  private String genericAttribute9;
  private String genericAttribute10;
  private String genericAttribute11;
  private String vendorCode;
  private String cartonReceived;
  private String volumeReceived;
  private String weightReceived;
  private String shipmentLineNumber;
  private String altItemNumber;
  private String subInventory;
  private String scannedUpc;
  private String pomType;
  private String pomMode;
  private String genericField1;
  private String genericField2;
  private String genericField3;
  private String qtyExpected;
  private String proNumber;
  private String genericField4;
  private String genericField5;
  private String genericField6;
  private String genericField7;
  private String genericField8;
  private String genericField9;
  private String genericField10;
  private String genericField11;
  private String genericField12;
  private String genericField13;
  private String genericField14;
  private String genericField15;
  private String trailerNumber;
  private Integer prePackQty;
  private String itemDescription;
  private String clientOrderNumber;
  private String fileSeq;
  private String receiptDatetime;
  private String expirationDate;
  private String damagedReasonCode;
  private String reasonGenericField1;
  private String processAttribute1;
  private String processAttribute2;
  private String processAttribute3;
  private String processAttribute4;
  private String processAttribute5;
  private String processAttribute6;
  private String processAttribute7;
  private String processAttribute8;
  private String processAttribute9;
  private String processAttribute10;
  private String processAttribute11;
  private String genericField16;
  private String genericField17;
  private String genericField18;
  private String genericField19;
  private String genericField20;
  private String genericField21;
  private String genericField22;
  private String genericField23;
  private String genericField24;
  private String genericField25;
  private String genericField26;
  private String genericField27;
  private String genericField28;
  private String genericField29;
  private String genericField30;
  private String huId2;
}
