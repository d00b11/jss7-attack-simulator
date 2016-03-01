package org.mobicents.protocols.ss7.tools.simulator.tests.attack;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LocationInfo;
import org.mobicents.protocols.ss7.isup.impl.message.parameter.LocationNumberImpl;
import org.mobicents.protocols.ss7.isup.message.parameter.LocationNumber;
import org.mobicents.protocols.ss7.map.api.*;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.errors.SMEnumeratedDeliveryFailureCause;
import org.mobicents.protocols.ss7.map.api.primitives.*;
import org.mobicents.protocols.ss7.map.api.service.callhandling.*;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPDialogMobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPServiceMobilityListener;
import org.mobicents.protocols.ss7.map.api.service.mobility.authentication.*;
import org.mobicents.protocols.ss7.map.api.service.mobility.faultRecovery.ForwardCheckSSIndicationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.faultRecovery.ResetRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.faultRecovery.RestoreDataRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.faultRecovery.RestoreDataResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.imei.CheckImeiRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.imei.CheckImeiResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.*;
import org.mobicents.protocols.ss7.map.api.service.mobility.oam.ActivateTraceModeRequest_Mobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.oam.ActivateTraceModeResponse_Mobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.*;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberManagement.*;
import org.mobicents.protocols.ss7.map.api.service.oam.*;
import org.mobicents.protocols.ss7.map.api.service.sms.*;
import org.mobicents.protocols.ss7.map.api.service.supplementary.*;
import org.mobicents.protocols.ss7.map.api.smstpdu.*;
import org.mobicents.protocols.ss7.map.primitives.CellGlobalIdOrServiceAreaIdFixedLengthImpl;
import org.mobicents.protocols.ss7.map.primitives.LAIFixedLengthImpl;
import org.mobicents.protocols.ss7.map.service.mobility.subscriberInformation.PDPContextInfoImpl;
import org.mobicents.protocols.ss7.map.smstpdu.*;
import org.mobicents.protocols.ss7.tcap.api.MessageType;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.mobicents.protocols.ss7.tools.simulator.Stoppable;
import org.mobicents.protocols.ss7.tools.simulator.common.AddressNatureType;
import org.mobicents.protocols.ss7.tools.simulator.common.AttackTesterBase;
import org.mobicents.protocols.ss7.tools.simulator.level3.MapMan;
import org.mobicents.protocols.ss7.tools.simulator.level3.MapProtocolVersion;
import org.mobicents.protocols.ss7.tools.simulator.level3.NumberingPlanMapType;
import org.mobicents.protocols.ss7.tools.simulator.management.AttackTesterHost;
import org.mobicents.protocols.ss7.tools.simulator.tests.sms.NumberingPlanIdentificationType;
import org.mobicents.protocols.ss7.tools.simulator.tests.sms.SmsCodingType;
import org.mobicents.protocols.ss7.tools.simulator.tests.sms.TypeOfNumberType;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;

/**
 * @author Kristoffer Jensen
 */
public class TestAttackServer extends AttackTesterBase implements Stoppable, MAPDialogListener, MAPServiceSmsListener,
        MAPServiceMobilityListener, MAPServiceCallHandlingListener, MAPServiceOamListener, MAPServiceSupplementaryListener {

    public static String SOURCE_NAME = "TestAttackServer";

    private final String name;
    private MapMan mapMan;

    private boolean isStarted = false;
    private int countSriReq = 0;
    private int countSriResp = 0;
    private int countMtFsmReq = 0;
    private int countMtFsmResp = 0;
    private int countMoFsmReq = 0;
    private int countMoFsmResp = 0;
    private int countIscReq = 0;
    private int countErrRcvd = 0;
    private int countErrSent = 0;
    private int countRsmdsReq = 0;
    private int countRsmdsResp = 0;
    private int countAscReq = 0;
    private int countAscResp = 0;
    private String currentRequestDef = "";
    private boolean needSendSend = false;
    private boolean needSendClose = false;

    private static Charset isoCharset = Charset.forName("ISO-8859-1");

    public TestAttackServer() {
        super(SOURCE_NAME);
        this.name = "???";
    }

    public TestAttackServer(String name) {
        super(SOURCE_NAME);
        this.name = name;
    }

    public void setTesterHost(AttackTesterHost testerHost) {
        this.testerHost = testerHost;
    }

    public void setMapMan(MapMan val) {
        this.mapMan = val;
    }

    public AddressNatureType getAddressNature() {
        return new AddressNatureType(this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getAddressNature().getIndicator());
    }

    public String getAddressNature_Value() {
        return new AddressNatureType(this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getAddressNature().getIndicator()).toString();
    }

    public void setAddressNature(AddressNatureType val) {
        this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().setAddressNature(AddressNature.getInstance(val.intValue()));
        this.testerHost.markStore();
    }

    public NumberingPlanMapType getNumberingPlan() {
        return new NumberingPlanMapType(this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getNumberingPlan().getIndicator());
    }

    public String getNumberingPlan_Value() {
        return new NumberingPlanMapType(this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getNumberingPlan().getIndicator())
                .toString();
    }

    public void setNumberingPlan(NumberingPlanMapType val) {
        this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().setNumberingPlan(NumberingPlan.getInstance(val.intValue()));
        this.testerHost.markStore();
    }

    public String getServiceCenterAddress() {
        return this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getServiceCenterAddress();
    }

    public void setServiceCenterAddress(String val) {
        this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().setServiceCenterAddress(val);
        this.testerHost.markStore();
    }

    public MapProtocolVersion getMapProtocolVersion() {
        return this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getMapProtocolVersion();
    }

    public String getMapProtocolVersion_Value() {
        return this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getMapProtocolVersion().toString();
    }

    public void setMapProtocolVersion(MapProtocolVersion val) {
        this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().setMapProtocolVersion(val);
        this.testerHost.markStore();
    }

    public int getHlrSsn() {
        return this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getHlrSsn();
    }

    public void setHlrSsn(int val) {
        this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().setHlrSsn(val);
        this.testerHost.markStore();
    }

    public int getVlrSsn() {
        return this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getVlrSsn();
    }

    public void setVlrSsn(int val) {
        this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().setVlrSsn(val);
        this.testerHost.markStore();
    }

    public TypeOfNumberType getTypeOfNumber() {
        return new TypeOfNumberType(this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getTypeOfNumber().getCode());
    }

    public String getTypeOfNumber_Value() {
        return new TypeOfNumberType(this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getTypeOfNumber().getCode()).toString();
    }

    public void setTypeOfNumber(TypeOfNumberType val) {
        this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().setTypeOfNumber(TypeOfNumber.getInstance(val.intValue()));
        this.testerHost.markStore();
    }

    public NumberingPlanIdentificationType getNumberingPlanIdentification() {
        return new NumberingPlanIdentificationType(this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getNumberingPlanIdentification()
                .getCode());
    }

    public String getNumberingPlanIdentification_Value() {
        return new NumberingPlanIdentificationType(this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getNumberingPlanIdentification()
                .getCode()).toString();
    }

    public void setNumberingPlanIdentification(NumberingPlanIdentificationType val) {
        this.testerHost.getConfigurationData().getTestAttackServerConfigurationData()
                .setNumberingPlanIdentification(NumberingPlanIdentification.getInstance(val.intValue()));
        this.testerHost.markStore();
    }

    public SmsCodingType getSmsCodingType() {
        return this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getSmsCodingType();
    }

    public String getSmsCodingType_Value() {
        return this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getSmsCodingType().toString();
    }

    public void setSmsCodingType(SmsCodingType val) {
        this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().setSmsCodingType(val);
        this.testerHost.markStore();
    }

    public boolean isSendSrsmdsIfError() {
        return this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().isSendSrsmdsIfError();
    }

    public void setSendSrsmdsIfError(boolean val) {
        this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().setSendSrsmdsIfError(val);
        this.testerHost.markStore();
    }

    public boolean isGprsSupportIndicator() {
        return this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().isGprsSupportIndicator();
    }

    public void setGprsSupportIndicator(boolean val) {
        this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().setGprsSupportIndicator(val);
        this.testerHost.markStore();
    }

    public void putAddressNature(String val) {
        AddressNatureType x = AddressNatureType.createInstance(val);
        if (x != null)
            this.setAddressNature(x);
    }

    public void putNumberingPlan(String val) {
        NumberingPlanMapType x = NumberingPlanMapType.createInstance(val);
        if (x != null)
            this.setNumberingPlan(x);
    }

    public void putMapProtocolVersion(String val) {
        MapProtocolVersion x = MapProtocolVersion.createInstance(val);
        if (x != null)
            this.setMapProtocolVersion(x);
    }

    public void putTypeOfNumber(String val) {
        TypeOfNumberType x = TypeOfNumberType.createInstance(val);
        if (x != null)
            this.setTypeOfNumber(x);
    }

    public void putNumberingPlanIdentification(String val) {
        NumberingPlanIdentificationType x = NumberingPlanIdentificationType.createInstance(val);
        if (x != null)
            this.setNumberingPlanIdentification(x);
    }

    public void putSmsCodingType(String val) {
        SmsCodingType x = SmsCodingType.createInstance(val);
        if (x != null)
            this.setSmsCodingType(x);
    }

    public String getCurrentRequestDef() {
        return "LastDialog: " + currentRequestDef;
    }

    public String getState() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append(SOURCE_NAME);
        sb.append(": ");
        sb.append("<br>Count: countSriReq-");
        sb.append(countSriReq);
        sb.append(", countSriResp-");
        sb.append(countSriResp);
        sb.append("<br>countMtFsmReq-");
        sb.append(countMtFsmReq);
        sb.append(", countMtFsmResp-");
        sb.append(countMtFsmResp);
        sb.append("<br> countMoFsmReq-");
        sb.append(countMoFsmReq);
        sb.append(", countMoFsmResp-");
        sb.append(countMoFsmResp);
        sb.append(", countIscReq-");
        sb.append(countIscReq);
        sb.append("<br>countRsmdsReq-");
        sb.append(countRsmdsReq);
        sb.append(", countRsmdsResp-");
        sb.append(countRsmdsResp);
        sb.append(", countAscReq-");
        sb.append(countAscReq);
        sb.append("<br>countAscResp-");
        sb.append(countAscResp);
        sb.append(", countErrRcvd-");
        sb.append(countErrRcvd);
        sb.append(", countErrSent-");
        sb.append(countErrSent);
        sb.append("</html>");
        return sb.toString();
    }

    public boolean start() {
        this.countSriReq = 0;
        this.countSriResp = 0;
        this.countMtFsmReq = 0;
        this.countMtFsmResp = 0;
        this.countMoFsmReq = 0;
        this.countMoFsmResp = 0;
        this.countIscReq = 0;
        this.countErrRcvd = 0;
        this.countErrSent = 0;
        this.countRsmdsReq = 0;
        this.countRsmdsResp = 0;
        this.countAscReq = 0;
        this.countAscResp = 0;

        MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();

        mapProvider.getMAPServiceSms().acivate();
        mapProvider.getMAPServiceSms().addMAPServiceListener(this);

        mapProvider.getMAPServiceMobility().acivate();
        mapProvider.getMAPServiceMobility().addMAPServiceListener(this);

        mapProvider.addMAPDialogListener(this);
        this.testerHost.sendNotif(SOURCE_NAME, "AttackServer has been started", "", Level.INFO);
        isStarted = true;

        return true;
    }

    public void stop() {
        MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();
        isStarted = false;
        mapProvider.getMAPServiceSms().deactivate();
        mapProvider.getMAPServiceSms().removeMAPServiceListener(this);
        mapProvider.removeMAPDialogListener(this);
        this.testerHost.sendNotif(SOURCE_NAME, "AttackServer has been stopped", "", Level.INFO);
    }

    public void execute() {
    }

    public String closeCurrentDialog() {
        // TODO Auto-generated method stub
        return null;
    }

    public String performSRIForSM(String destIsdnNumber) {
        if (!isStarted)
            return "The tester is not started";
        if (destIsdnNumber == null || destIsdnNumber.equals(""))
            return "DestIsdnNumber is empty";

        currentRequestDef = "";

        return doSendSri(destIsdnNumber, this.getServiceCenterAddress(), null);
    }

    private String curDestIsdnNumber = null;

    private String doSendSri(String destIsdnNumber, String serviceCentreAddr, MtMessageData messageData) {

        MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();

        MAPApplicationContextVersion vers;
        switch (this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getMapProtocolVersion().intValue()) {
            case MapProtocolVersion.VAL_MAP_V1:
                vers = MAPApplicationContextVersion.version1;
                break;
            case MapProtocolVersion.VAL_MAP_V2:
                vers = MAPApplicationContextVersion.version2;
                break;
            default:
                vers = MAPApplicationContextVersion.version3;
                break;
        }
        MAPApplicationContext mapAppContext = MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgGatewayContext, vers);

        ISDNAddressString msisdn = mapProvider.getMAPParameterFactory().createISDNAddressString(
                this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getAddressNature(),
                this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getNumberingPlan(), destIsdnNumber);
        AddressString serviceCentreAddress = mapProvider.getMAPParameterFactory().createAddressString(
                this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getAddressNature(),
                this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getNumberingPlan(), serviceCentreAddr);
        curDestIsdnNumber = destIsdnNumber;

        try {
            MAPDialogSms curDialog = mapProvider.getMAPServiceSms()
                    .createNewDialog(
                            mapAppContext,
                            this.mapMan.createOrigAddress(),
                            null,
                            this.mapMan.createDestAddress(destIsdnNumber, this.testerHost.getConfigurationData().getTestAttackServerConfigurationData()
                                    .getHlrSsn()), null);
            HostMessageData hostMessageData = new HostMessageData();
            hostMessageData.mtMessageData = messageData;
            curDialog.setUserObject(hostMessageData);

            curDialog.addSendRoutingInfoForSMRequest(msisdn, true, serviceCentreAddress, null, this.testerHost.getConfigurationData()
                    .getTestAttackServerConfigurationData().isGprsSupportIndicator(), null, null, null);

            // this cap helps us give SCCP error if any
            // curDialog.setReturnMessageOnError(true);

            curDialog.send();

            String sriData = createSriData(curDialog.getLocalDialogId(), destIsdnNumber, serviceCentreAddr);
            currentRequestDef += "Sent SriReq;";
            this.countSriReq++;
            this.testerHost.sendNotif(SOURCE_NAME, "Sent: sriReq", sriData, Level.DEBUG);

            return "SendRoutingInfoForSMRequest has been sent";
        } catch (MAPException ex) {
            return "Exception when sending SendRoutingInfoForSMRequest: " + ex.toString();
        }
    }

    private String createSriData(long dialogId, String destIsdnNumber, String serviceCentreAddr) {
        StringBuilder sb = new StringBuilder();
        sb.append("dialogId=");
        sb.append(dialogId);
        sb.append(", destIsdnNumber=\"");
        sb.append(destIsdnNumber);
        sb.append("\", serviceCentreAddr=\"");
        sb.append(serviceCentreAddr);
        sb.append("\"");
        return sb.toString();
    }

    public String performSRIForSM_MtForwardSM(String msg, String destIsdnNumber, String origIsdnNumber) {
        if (!isStarted)
            return "The tester is not started";
        if (origIsdnNumber == null || origIsdnNumber.equals(""))
            return "OrigIsdnNumber is empty";
        if (destIsdnNumber == null || destIsdnNumber.equals(""))
            return "DestIsdnNumber is empty";
        if (msg == null || msg.equals(""))
            return "Msg is empty";
        int maxMsgLen = this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getSmsCodingType().getSupportesMaxMessageLength(0);
        if (msg.length() > maxMsgLen)
            return "Simulator does not support message length for current encoding type more than " + maxMsgLen;

        currentRequestDef = "";

        MtMessageData mmd = new MtMessageData();
        mmd.msg = msg;
        mmd.origIsdnNumber = origIsdnNumber;

        return doSendSri(destIsdnNumber, this.getServiceCenterAddress(), mmd);
    }

    public String performMtForwardSM(String msg, String destImsi, String vlrNumber, String origIsdnNumber) {
        if (!isStarted)
            return "The tester is not started";
        if (msg == null || msg.equals(""))
            return "Msg is empty";
        if (destImsi == null || destImsi.equals(""))
            return "DestImsi is empty";
        if (vlrNumber == null || vlrNumber.equals(""))
            return "VlrNumber is empty";
        if (origIsdnNumber == null || origIsdnNumber.equals(""))
            return "OrigIsdnNumber is empty";
        int maxMsgLen = this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getSmsCodingType().getSupportesMaxMessageLength(0);
        if (msg.length() > maxMsgLen)
            return "Simulator does not support message length for current encoding type more than " + maxMsgLen;

        currentRequestDef = "";

        return doMtForwardSM(msg, destImsi, vlrNumber, origIsdnNumber, this.getServiceCenterAddress());
    }

    private String doMtForwardSM(String msg, String destImsi, String vlrNumber, String origIsdnNumber, String serviceCentreAddr) {

        MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();

        MAPApplicationContextVersion vers;
        MAPApplicationContextName acn = MAPApplicationContextName.shortMsgMTRelayContext;
        switch (this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getMapProtocolVersion().intValue()) {
            case MapProtocolVersion.VAL_MAP_V1:
                vers = MAPApplicationContextVersion.version1;
                acn = MAPApplicationContextName.shortMsgMORelayContext;
                break;
            case MapProtocolVersion.VAL_MAP_V2:
                vers = MAPApplicationContextVersion.version2;
                break;
            default:
                vers = MAPApplicationContextVersion.version3;
                break;
        }
        MAPApplicationContext mapAppContext = MAPApplicationContext.getInstance(acn, vers);

        IMSI imsi = mapProvider.getMAPParameterFactory().createIMSI(destImsi);
        SM_RP_DA da = mapProvider.getMAPParameterFactory().createSM_RP_DA(imsi);
        AddressString serviceCentreAddress = mapProvider.getMAPParameterFactory().createAddressString(
                this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getAddressNature(),
                this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getNumberingPlan(), serviceCentreAddr);
        SM_RP_OA oa = mapProvider.getMAPParameterFactory().createSM_RP_OA_ServiceCentreAddressOA(serviceCentreAddress);

        try {
            AddressField originatingAddress = new AddressFieldImpl(
                    this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getTypeOfNumber(), this.testerHost.getConfigurationData()
                    .getTestAttackServerConfigurationData().getNumberingPlanIdentification(), origIsdnNumber);
            Calendar cld = new GregorianCalendar();
            int year = cld.get(Calendar.YEAR);
            int mon = cld.get(Calendar.MONTH);
            int day = cld.get(Calendar.DAY_OF_MONTH);
            int h = cld.get(Calendar.HOUR);
            int m = cld.get(Calendar.MINUTE);
            int s = cld.get(Calendar.SECOND);
            int tz = cld.get(Calendar.ZONE_OFFSET);
            AbsoluteTimeStamp serviceCentreTimeStamp = new AbsoluteTimeStampImpl(year - 2000, mon, day, h, m, s, tz / 1000 / 60 / 15);

            int dcsVal = 0;
            switch (this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getSmsCodingType().intValue()) {
                case SmsCodingType.VAL_GSM7:
                    dcsVal = 0;
                    break;
                case SmsCodingType.VAL_GSM8:
                    dcsVal = 4;
                    break;
                case SmsCodingType.VAL_UCS2:
                    dcsVal = 8;
                    break;
            }
            DataCodingScheme dcs = new DataCodingSchemeImpl(dcsVal);

            UserDataHeader udh = null;
            if (dcs.getCharacterSet() == CharacterSet.GSM8) {
                ApplicationPortAddressing16BitAddressImpl apa16 = new ApplicationPortAddressing16BitAddressImpl(16020, 0);
                udh = new UserDataHeaderImpl();
                udh.addInformationElement(apa16);
            }

            UserData userData = new UserDataImpl(msg, dcs, udh, isoCharset);
            ProtocolIdentifier pi = new ProtocolIdentifierImpl(0);
            SmsDeliverTpdu tpdu = new SmsDeliverTpduImpl(false, false, false, false, originatingAddress, pi, serviceCentreTimeStamp, userData);
            SmsSignalInfo si = mapProvider.getMAPParameterFactory().createSmsSignalInfo(tpdu, null);

            MAPDialogSms curDialog = mapProvider.getMAPServiceSms().createNewDialog(mapAppContext, this.mapMan.createOrigAddress(), null,
                    this.mapMan.createDestAddress(vlrNumber, this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getVlrSsn()), null);

            if (si.getData().length < 110 || vers == MAPApplicationContextVersion.version1) {
                if (this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getMapProtocolVersion().intValue() <= 2)
                    curDialog.addForwardShortMessageRequest(da, oa, si, false);
                else
                    curDialog.addMtForwardShortMessageRequest(da, oa, si, false, null);
                curDialog.send();

                String mtData = createMtData(curDialog.getLocalDialogId(), destImsi, vlrNumber, origIsdnNumber, serviceCentreAddr);
                currentRequestDef += "Sent mtReq;";
                this.countMtFsmReq++;
                this.testerHost.sendNotif(SOURCE_NAME, "Sent: mtReq: " + msg, mtData, Level.DEBUG);
            } else {
                ResendMessageData md = new ResendMessageData();
                md.da = da;
                md.oa = oa;
                md.si = si;
                md.destImsi = destImsi;
                md.vlrNumber = vlrNumber;
                md.origIsdnNumber = origIsdnNumber;
                md.serviceCentreAddr = serviceCentreAddr;
                md.msg = msg;

                HostMessageData hmd = (HostMessageData) curDialog.getUserObject();
                if (hmd == null) {
                    hmd = new HostMessageData();
                    curDialog.setUserObject(hmd);
                }
                hmd.resendMessageData = md;

                curDialog.send();
                currentRequestDef += "Sent emptTBegin;";
                this.testerHost.sendNotif(SOURCE_NAME, "Sent: emptTBegin", "", Level.DEBUG);
            }

            return "MtForwardShortMessageRequest has been sent";
        } catch (MAPException ex) {
            return "Exception when sending MtForwardShortMessageRequest: " + ex.toString();
        }
    }

    private String createMtData(long dialogId, String destImsi, String vlrNumber, String origIsdnNumber, String serviceCentreAddr) {
        StringBuilder sb = new StringBuilder();
        sb.append("dialogId=");
        sb.append(dialogId);
        sb.append(", destImsi=\"");
        sb.append(destImsi);
        sb.append(", vlrNumber=\"");
        sb.append(vlrNumber);
        sb.append(", origIsdnNumber=\"");
        sb.append(origIsdnNumber);
        sb.append("\", serviceCentreAddr=\"");
        sb.append(serviceCentreAddr);
        sb.append("\"");
        return sb.toString();
    }

    @Override
    public void onForwardShortMessageRequest(ForwardShortMessageRequest ind) {
        if (!isStarted)
            return;

        MAPDialogSms curDialog = ind.getMAPDialog();
        long invokeId = ind.getInvokeId();
        SM_RP_DA da = ind.getSM_RP_DA();
        SM_RP_OA oa = ind.getSM_RP_OA();
        SmsSignalInfo si = ind.getSM_RP_UI();

        if (da.getServiceCentreAddressDA() != null) { // mo message
            this.onMoRequest(da, oa, si, curDialog);

            try {
                curDialog.addForwardShortMessageResponse(invokeId);
                this.needSendClose = true;

                this.countMoFsmResp++;
                this.testerHost.sendNotif(SOURCE_NAME, "Sent: moResp", "", Level.DEBUG);
            } catch (MAPException e) {
                this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking addMoForwardShortMessageResponse : " + e.getMessage(), e, Level.ERROR);
            }
        }
    }

    @Override
    public void onForwardShortMessageResponse(ForwardShortMessageResponse ind) {
        if (!isStarted)
            return;

        this.countMtFsmResp++;

        MAPDialogSms curDialog = ind.getMAPDialog();
        long invokeId = curDialog.getLocalDialogId();
        currentRequestDef += "Rsvd mtResp;";
        this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: mtResp", "", Level.DEBUG);

        if (ind.getMAPDialog().getTCAPMessageType() == MessageType.Continue) {
            needSendClose = true;
        }
    }

    @Override
    public void onMoForwardShortMessageRequest(MoForwardShortMessageRequest ind) {
        if (!isStarted)
            return;

        MAPDialogSms curDialog = ind.getMAPDialog();
        long invokeId = ind.getInvokeId();
        SM_RP_DA da = ind.getSM_RP_DA();
        SM_RP_OA oa = ind.getSM_RP_OA();
        SmsSignalInfo si = ind.getSM_RP_UI();

        this.onMoRequest(da, oa, si, curDialog);

        try {
            curDialog.addMoForwardShortMessageResponse(invokeId, null, null);
            this.needSendClose = true;

            this.countMoFsmResp++;
            this.testerHost.sendNotif(SOURCE_NAME, "Sent: moResp", "", Level.DEBUG);
        } catch (MAPException e) {
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking addMoForwardShortMessageResponse : " + e.getMessage(), e, Level.ERROR);
        }
    }

    private void onMoRequest(SM_RP_DA da, SM_RP_OA oa, SmsSignalInfo si, MAPDialogSms curDialog) {

        this.countMoFsmReq++;

        si.setGsm8Charset(isoCharset);
        String serviceCentreAddr = null;
        if (da != null) {
            AddressString as = da.getServiceCentreAddressDA();
            if (as != null)
                serviceCentreAddr = as.getAddress();
        }

        String origIsdnNumber = null;
        if (oa != null) {
            ISDNAddressString isdn = oa.getMsisdn();
            if (isdn != null)
                origIsdnNumber = isdn.getAddress();
        }

        try {
            String msg = null;
            String destIsdnNumber = null;
            if (si != null) {
                SmsTpdu tpdu = si.decodeTpdu(true);
                if (tpdu instanceof SmsSubmitTpdu) {
                    SmsSubmitTpdu dTpdu = (SmsSubmitTpdu) tpdu;
                    AddressField af = dTpdu.getDestinationAddress();
                    if (af != null)
                        destIsdnNumber = af.getAddressValue();
                    UserData ud = dTpdu.getUserData();
                    if (ud != null) {
                        ud.decode();
                        msg = ud.getDecodedMessage();

                        UserDataHeader udh = ud.getDecodedUserDataHeader();
                        if (udh != null) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("[");
                            int i2 = 0;
                            for (byte b : udh.getEncodedData()) {
                                int i1 = (b & 0xFF);
                                if (i2 == 0)
                                    i2 = 1;
                                else
                                    sb.append(", ");
                                sb.append(i1);
                            }
                            sb.append("] ");
                            msg = sb.toString() + msg;
                        }
                    }
                }
            }
            String uData = this.createMoData(curDialog.getLocalDialogId(), destIsdnNumber, origIsdnNumber, serviceCentreAddr);
            this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: moReq: " + msg, uData, Level.DEBUG);
        } catch (MAPException e) {
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when decoding MoForwardShortMessageRequest tpdu : " + e.getMessage(), e, Level.ERROR);
        }
    }

    private String createMoData(long dialogId, String destIsdnNumber, String origIsdnNumber, String serviceCentreAddr) {
        StringBuilder sb = new StringBuilder();
        sb.append("dialogId=");
        sb.append(dialogId);
        sb.append(", destIsdnNumber=\"");
        sb.append(destIsdnNumber);
        sb.append(", origIsdnNumber=\"");
        sb.append(origIsdnNumber);
        sb.append("\", serviceCentreAddr=\"");
        sb.append(serviceCentreAddr);
        sb.append("\"");
        return sb.toString();
    }

    @Override
    public void onMoForwardShortMessageResponse(MoForwardShortMessageResponse moForwSmRespInd) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMtForwardShortMessageRequest(MtForwardShortMessageRequest mtForwSmInd) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMtForwardShortMessageResponse(MtForwardShortMessageResponse ind) {
        if (!isStarted)
            return;

        this.countMtFsmResp++;

        MAPDialogSms curDialog = ind.getMAPDialog();
        long invokeId = curDialog.getLocalDialogId();
        currentRequestDef += "Rsvd mtResp;";
        this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: mtResp", "", Level.DEBUG);

        if (ind.getMAPDialog().getTCAPMessageType() == MessageType.Continue) {
            needSendClose = true;
        }
    }

    @Override
    public void onSendRoutingInfoForSMRequest(SendRoutingInfoForSMRequest sendRoutingInfoForSMInd) {
        MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();
        MAPDialogSms curDialog = sendRoutingInfoForSMInd.getMAPDialog();
        long invokeId = sendRoutingInfoForSMInd.getInvokeId();


        IMSI imsi = mapProvider.getMAPParameterFactory().createIMSI(
                this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getSriResponseImsi());
        ISDNAddressString networkNodeNumber = mapProvider.getMAPParameterFactory().createISDNAddressString(
                this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getAddressNature(),
                this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getNumberingPlan(),
                this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getSriResponseVlr());
        LocationInfoWithLMSI li = null;

        try {
            li = mapProvider.getMAPParameterFactory().createLocationInfoWithLMSI(networkNodeNumber, null, null, false, null);
            curDialog.addSendRoutingInfoForSMResponse(invokeId, imsi, li, null, null);

            this.needSendClose = true;

        } catch (MAPException e) {
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking addSendRoutingInfoForSMResponse() : " + e.getMessage(), e, Level.ERROR);
        }
    }

    @Override
    public void onSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse ind) {
        if (!isStarted)
            return;

        this.countSriResp++;

        MAPDialogSms curDialog = ind.getMAPDialog();
        long invokeId = curDialog.getLocalDialogId();
        LocationInfoWithLMSI li = ind.getLocationInfoWithLMSI();
        String vlrNum = "";
        if (li != null && li.getNetworkNodeNumber() != null)
            vlrNum = li.getNetworkNodeNumber().getAddress();
        currentRequestDef += "Rsvd SriResp;";
        String destImsi = "";
        if (ind.getIMSI() != null)
            destImsi = ind.getIMSI().getData();
        String uData = this.createSriRespData(invokeId, ind);
        this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: sriResp", uData, Level.DEBUG);

        if (curDialog.getUserObject() != null && vlrNum != null && !vlrNum.equals("") && destImsi != null && !destImsi.equals("")) {
            HostMessageData hmd = (HostMessageData) curDialog.getUserObject();
            MtMessageData mmd = hmd.mtMessageData;
            if (mmd != null) {
                mmd.vlrNum = vlrNum;
                mmd.destImsi = destImsi;
            }

            // // sending SMS
            // doMtForwardSM(mmd.msg, destImsi, vlrNum, mmd.origIsdnNumber,
            // this.testerHost.getConfigurationData().getTestAttackServerConfigurationData()
            // .getServiceCenterAddress());
        }
    }

    private String createSriRespData(long dialogId, SendRoutingInfoForSMResponse ind) {
        StringBuilder sb = new StringBuilder();
        sb.append("dialogId=");
        sb.append(dialogId);
        sb.append(", ind=\"");
        sb.append(ind);
        sb.append("\"");
        return sb.toString();
    }

    private String createIscReqData(long dialogId, MWStatus mwStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append("dialogId=");
        sb.append(dialogId);
        sb.append(",\n mwStatus=");
        sb.append(mwStatus);
        sb.append(",\n");
        return sb.toString();
    }

    @Override
    public void onReportSMDeliveryStatusRequest(ReportSMDeliveryStatusRequest reportSMDeliveryStatusInd) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onReportSMDeliveryStatusResponse(ReportSMDeliveryStatusResponse ind) {
        if (!isStarted)
            return;

        this.countRsmdsResp++;

        MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();
        MAPDialogSms curDialog = ind.getMAPDialog();
        long invokeId = ind.getInvokeId();

        this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: rsmdsResp", ind.toString(), Level.DEBUG);
    }

    @Override
    public void onInformServiceCentreRequest(InformServiceCentreRequest ind) {
        if (!isStarted)
            return;

        this.countSriResp++;
        currentRequestDef += "Rsvd IscReq;";

        MAPDialogSms curDialog = ind.getMAPDialog();
        long invokeId = curDialog.getLocalDialogId();
        MWStatus mwStatus = ind.getMwStatus();
        String uData = this.createIscReqData(invokeId, mwStatus);
        this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: iscReq", uData, Level.DEBUG);
    }

    @Override
    public void onAlertServiceCentreRequest(AlertServiceCentreRequest ind) {
        if (!isStarted)
            return;

        this.countAscReq++;

        MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();
        MAPDialogSms curDialog = ind.getMAPDialog();
        long invokeId = ind.getInvokeId();

        this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: ascReq", ind.toString(), Level.DEBUG);

        try {
            if (curDialog.getApplicationContext().getApplicationContextVersion() == MAPApplicationContextVersion.version1) {
                curDialog.release();
            } else {
                curDialog.addAlertServiceCentreResponse(invokeId);

                this.countAscResp++;
                this.testerHost.sendNotif(SOURCE_NAME, "Sent: ascResp", "", Level.DEBUG);

                this.needSendClose = true;
            }

        } catch (MAPException e) {
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking addAlertServiceCentreResponse() : " + e.getMessage(), e, Level.ERROR);
        }
    }

    @Override
    public void onAlertServiceCentreResponse(AlertServiceCentreResponse alertServiceCentreInd) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDialogRequest(MAPDialog arg0, AddressString arg1, AddressString arg2, MAPExtensionContainer arg3) {
        int i1 = 0;
    }

    @Override
    public void onDialogDelimiter(MAPDialog mapDialog) {

        if (mapDialog.getApplicationContext().getApplicationContextName() == MAPApplicationContextName.shortMsgMTRelayContext
                || mapDialog.getApplicationContext().getApplicationContextName() == MAPApplicationContextName.shortMsgMORelayContext) {
            if (mapDialog.getUserObject() != null) {
                HostMessageData hmd = (HostMessageData) mapDialog.getUserObject();
                ResendMessageData md = hmd.resendMessageData;
                if (md != null) {
                    try {
                        MAPDialogSms dlg = (MAPDialogSms) mapDialog;

                        if (dlg.getApplicationContext().getApplicationContextVersion().getVersion() <= 2)
                            dlg.addForwardShortMessageRequest(md.da, md.oa, md.si, false);
                        else
                            dlg.addMoForwardShortMessageRequest(md.da, md.oa, md.si, null, null);
                        mapDialog.send();

                        String mtData = createMtData(mapDialog.getLocalDialogId(), md.destImsi, md.vlrNumber, md.origIsdnNumber, md.serviceCentreAddr);
                        currentRequestDef += "Rcvd emptTCont;Sent moReq;";
                        this.countMoFsmReq++;
                        this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: emptTCont", "", Level.DEBUG);
                        this.testerHost.sendNotif(SOURCE_NAME, "Sent: moReq: " + md.msg, mtData, Level.DEBUG);
                    } catch (Exception e) {
                        this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking close() : " + e.getMessage(), e, Level.ERROR);
                        return;
                    }
                    hmd.resendMessageData = null;
                    return;
                }
            }
        }

        try {
            if (needSendSend) {
                needSendSend = false;
                mapDialog.send();
                return;
            }
        } catch (Exception e) {
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking send() : " + e.getMessage(), e, Level.ERROR);
            return;
        }
        try {
            if (needSendClose) {
                needSendClose = false;
                mapDialog.close(false);
                return;
            }
        } catch (Exception e) {
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking close() : " + e.getMessage(), e, Level.ERROR);
            return;
        }

        if (mapDialog.getApplicationContext().getApplicationContextName() == MAPApplicationContextName.shortMsgMTRelayContext
                || mapDialog.getApplicationContext().getApplicationContextName() == MAPApplicationContextName.shortMsgMORelayContext) {
            // this is an empty first TC-BEGIN for MO SMS
            try {
                mapDialog.send();
                currentRequestDef += "Rcvd emptTBeg;Sent emptTCont;";
                this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: emptTBeg", "", Level.DEBUG);
                this.testerHost.sendNotif(SOURCE_NAME, "Sent: emptTCont", "", Level.DEBUG);
            } catch (Exception e) {
                this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking send() : " + e.getMessage(), e, Level.ERROR);
            }
            return;
        }
    }

    @Override
    public void onDialogClose(MAPDialog mapDialog) {
        if (mapDialog.getUserObject() != null) {
            HostMessageData hmd = (HostMessageData) mapDialog.getUserObject();
            MtMessageData mmd = hmd.mtMessageData;
            if (mmd != null && mmd.vlrNum != null && mmd.destImsi != null) {
                // sending SMS
                doMtForwardSM(mmd.msg, mmd.destImsi, mmd.vlrNum, mmd.origIsdnNumber, this.testerHost.getConfigurationData().getTestAttackServerConfigurationData()
                        .getServiceCenterAddress());
            }
        }

        try {
            if (needSendSend) {
                needSendSend = false;
                mapDialog.send();
            }
        } catch (Exception e) {
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking send() : " + e.getMessage(), e, Level.ERROR);
        }
        try {
            if (needSendClose) {
                needSendClose = false;
                mapDialog.close(false);
            }
        } catch (Exception e) {
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking close() : " + e.getMessage(), e, Level.ERROR);
        }
    }

    @Override
    public void onErrorComponent(MAPDialog dlg, Long invokeId, MAPErrorMessage msg) {
        // if an error for (mt)ForwardSM or SRI requests
        if (dlg.getApplicationContext().getApplicationContextName() != MAPApplicationContextName.shortMsgMTRelayContext
                || dlg.getApplicationContext().getApplicationContextName() != MAPApplicationContextName.shortMsgMORelayContext
                || (dlg.getUserObject() != null && ((HostMessageData) dlg.getUserObject()).mtMessageData != null && ((HostMessageData) dlg.getUserObject()).mtMessageData.msg != null)) {
            if (this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().isSendSrsmdsIfError() && curDestIsdnNumber != null) {
                try {
                    MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();
                    MAPApplicationContextVersion vers = dlg.getApplicationContext().getApplicationContextVersion();
                    MAPApplicationContext mapAppContext = MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgGatewayContext, vers);

                    MAPDialogSms curDialog = mapProvider.getMAPServiceSms().createNewDialog(
                            mapAppContext,
                            this.mapMan.createOrigAddress(),
                            null,
                            this.mapMan.createDestAddress(curDestIsdnNumber, this.testerHost.getConfigurationData().getTestAttackServerConfigurationData()
                                    .getHlrSsn()), null);

                    ISDNAddressString msisdn = mapProvider.getMAPParameterFactory().createISDNAddressString(
                            this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getAddressNature(),
                            this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getNumberingPlan(), curDestIsdnNumber);
                    AddressString serviceCentreAddress = mapProvider.getMAPParameterFactory().createAddressString(
                            this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getAddressNature(),
                            this.testerHost.getConfigurationData().getTestAttackServerConfigurationData().getNumberingPlan(), this.getServiceCenterAddress());
                    curDestIsdnNumber = null;

                    SMDeliveryOutcome sMDeliveryOutcome = null;
                    if (vers.getVersion() >= 2) {
                        if (msg.isEmSMDeliveryFailure()
                                && msg.getEmSMDeliveryFailure().getSMEnumeratedDeliveryFailureCause() == SMEnumeratedDeliveryFailureCause.memoryCapacityExceeded)
                            sMDeliveryOutcome = SMDeliveryOutcome.memoryCapacityExceeded;
                        else
                            sMDeliveryOutcome = SMDeliveryOutcome.absentSubscriber;
                    }

                    curDialog.addReportSMDeliveryStatusRequest(msisdn, serviceCentreAddress, sMDeliveryOutcome, null, null, false, false, null, null);
                    curDialog.send();

                    currentRequestDef += "Sent RsmdsReq;";
                    this.countRsmdsReq++;
                    String rsmdsData = "msisdn=" + msisdn + ", serviceCentreAddress=" + serviceCentreAddress + ", sMDeliveryOutcome=" + sMDeliveryOutcome;
                    this.testerHost.sendNotif(SOURCE_NAME, "Sent: rsmdsReq", rsmdsData, Level.DEBUG);
                } catch (MAPException e) {
                    this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking reportSMDeliveryStatusRequest : " + e.getMessage(), e, Level.ERROR);
                }
            }
        }

        super.onErrorComponent(dlg, invokeId, msg);

        // needSendClose = true;
    }

    @Override
    public void onRejectComponent(MAPDialog mapDialog, Long invokeId, Problem problem, boolean isLocalOriginated) {
        super.onRejectComponent(mapDialog, invokeId, problem, isLocalOriginated);
        if (isLocalOriginated)
            needSendClose = true;
    }

    @Override
    public void onUpdateLocationRequest(UpdateLocationRequest ind) {

    }

    @Override
    public void onUpdateLocationResponse(UpdateLocationResponse ind) {

    }

    @Override
    public void onCancelLocationRequest(CancelLocationRequest request) {

    }

    @Override
    public void onCancelLocationResponse(CancelLocationResponse response) {

    }

    @Override
    public void onSendIdentificationRequest(SendIdentificationRequest request) {

    }

    @Override
    public void onSendIdentificationResponse(SendIdentificationResponse response) {

    }

    @Override
    public void onUpdateGprsLocationRequest(UpdateGprsLocationRequest request) {

    }

    @Override
    public void onUpdateGprsLocationResponse(UpdateGprsLocationResponse response) {

    }

    @Override
    public void onPurgeMSRequest(PurgeMSRequest request) {

    }

    @Override
    public void onPurgeMSResponse(PurgeMSResponse response) {

    }

    @Override
    public void onSendAuthenticationInfoRequest(SendAuthenticationInfoRequest ind) {

    }

    @Override
    public void onSendAuthenticationInfoResponse(SendAuthenticationInfoResponse ind) {

    }

    @Override
    public void onAuthenticationFailureReportRequest(AuthenticationFailureReportRequest ind) {

    }

    @Override
    public void onAuthenticationFailureReportResponse(AuthenticationFailureReportResponse ind) {

    }

    @Override
    public void onResetRequest(ResetRequest ind) {

    }

    @Override
    public void onForwardCheckSSIndicationRequest(ForwardCheckSSIndicationRequest ind) {

    }

    @Override
    public void onRestoreDataRequest(RestoreDataRequest ind) {

    }

    @Override
    public void onRestoreDataResponse(RestoreDataResponse ind) {

    }

    @Override
    public void onAnyTimeInterrogationRequest(AnyTimeInterrogationRequest request) {

    }

    @Override
    public void onAnyTimeInterrogationResponse(AnyTimeInterrogationResponse response) {

    }

    @Override
    public void onProvideSubscriberInfoRequest(ProvideSubscriberInfoRequest request) {
        if (!isStarted)
            return;

        MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();
        MAPParameterFactory mapParameterFactory = mapProvider.getMAPParameterFactory();
        MAPDialogMobility curDialog = request.getMAPDialog();
        long invokeId = request.getInvokeId();

        try {
            LocationInformation locationInformation = this.createLocationInformation(mapParameterFactory);
            SubscriberState subscriberState = mapParameterFactory.createSubscriberState(SubscriberStateChoice.assumedIdle, null);
            MAPExtensionContainer mapExtensionContainer = null;
            LocationInformationGPRS locationInformationGPRS = null;
            PSSubscriberState psSubscriberState = null;
            IMEI imei = mapParameterFactory.createIMEI("128939812312382");
            MSClassmark2 msClassmark2 = null;
            GPRSMSClass gprsmsClass = null;
            MNPInfoRes mnpInfoRes = null;


            //SubscriberInfo subscriberInfo = mapParameterFactory.createSubscriberInfo(locationInformation, subscriberState,
            //    mapExtensionContainer, locationInformationGPRS, psSubscriberState, imei, msClassmark2, gprsmsClass,
            //    mnpInfoRes);
            SubscriberInfo subscriberInfo = mapParameterFactory.createSubscriberInfo(locationInformation,
                    subscriberState,
                    mapExtensionContainer,
                    locationInformationGPRS,
                    psSubscriberState,
                    imei,
                    msClassmark2,
                    gprsmsClass,
                    mnpInfoRes);


            curDialog.addProvideSubscriberInfoResponse(invokeId, subscriberInfo, null);
            this.needSendClose = true;
        } catch (MAPException ex) {
            System.out.println("Exception when sending ProvideSubscriberInfoRequestRes: " + ex.toString());
        }
    }

    private LocationInformation createLocationInformation(MAPParameterFactory mapParameterFactory) throws MAPException {
        int ageOfLocationInformation = 0;
        GeographicalInformation geographicalInformation = null;
        ISDNAddressString vlrNumber = mapParameterFactory.createISDNAddressString(AddressNature.international_number, NumberingPlan.ISDN, "22222222");
        LocationNumberMap locationNumber = mapParameterFactory.createLocationNumberMap(new LocationNumberImpl(4,
                "88888888",
                LocationNumber._NPI_ISDN,
                LocationNumber._INN_ROUTING_ALLOWED,
                LocationNumber._APRI_ALLOWED,
                LocationNumber._SI_USER_PROVIDED_VERIFIED_PASSED));
        CellGlobalIdOrServiceAreaIdOrLAI cgiosaiol = mapParameterFactory.createCellGlobalIdOrServiceAreaIdOrLAI(
                new CellGlobalIdOrServiceAreaIdFixedLengthImpl(242,01, 115, 8462));
        MAPExtensionContainer mapExtensionContainer = null;
        LSAIdentity lsaIdentity = null;
        ISDNAddressString mscNumber = null;
        GeodeticInformation geodeticInformation = null;
        boolean currentLocationRetrieved = true;
        boolean saiPresent = false;
        LocationInformationEPS locationInformationEPS = null;
        UserCSGInformation userCSGInformation = null;


        return mapParameterFactory.createLocationInformation(ageOfLocationInformation, geographicalInformation,
                vlrNumber, locationNumber, cgiosaiol, mapExtensionContainer,lsaIdentity,
                mscNumber, geodeticInformation, currentLocationRetrieved, saiPresent, locationInformationEPS,
                userCSGInformation);
    }


    @Override
    public void onProvideSubscriberInfoResponse(ProvideSubscriberInfoResponse response) {

    }

    @Override
    public void onInsertSubscriberDataRequest(InsertSubscriberDataRequest request) {

    }

    @Override
    public void onInsertSubscriberDataResponse(InsertSubscriberDataResponse request) {

    }

    @Override
    public void onDeleteSubscriberDataRequest(DeleteSubscriberDataRequest request) {

    }

    @Override
    public void onDeleteSubscriberDataResponse(DeleteSubscriberDataResponse request) {

    }

    @Override
    public void onCheckImeiRequest(CheckImeiRequest request) {

    }

    @Override
    public void onCheckImeiResponse(CheckImeiResponse response) {

    }

    @Override
    public void onActivateTraceModeRequest_Mobility(ActivateTraceModeRequest_Mobility ind) {

    }

    @Override
    public void onActivateTraceModeResponse_Mobility(ActivateTraceModeResponse_Mobility ind) {

    }

    @Override
    public void onSendRoutingInformationRequest(SendRoutingInformationRequest request) {

    }

    @Override
    public void onSendRoutingInformationResponse(SendRoutingInformationResponse response) {

    }

    @Override
    public void onProvideRoamingNumberRequest(ProvideRoamingNumberRequest request) {

    }

    @Override
    public void onProvideRoamingNumberResponse(ProvideRoamingNumberResponse response) {

    }

    @Override
    public void onActivateTraceModeRequest_Oam(ActivateTraceModeRequest_Oam ind) {

    }

    @Override
    public void onActivateTraceModeResponse_Oam(ActivateTraceModeResponse_Oam ind) {

    }

    @Override
    public void onSendImsiRequest(SendImsiRequest ind) {

    }

    @Override
    public void onSendImsiResponse(SendImsiResponse ind) {

    }

    @Override
    public void onRegisterSSRequest(RegisterSSRequest request) {

    }

    @Override
    public void onRegisterSSResponse(RegisterSSResponse response) {

    }

    @Override
    public void onEraseSSRequest(EraseSSRequest request) {

    }

    @Override
    public void onEraseSSResponse(EraseSSResponse response) {

    }

    @Override
    public void onActivateSSRequest(ActivateSSRequest request) {

    }

    @Override
    public void onActivateSSResponse(ActivateSSResponse response) {

    }

    @Override
    public void onDeactivateSSRequest(DeactivateSSRequest request) {

    }

    @Override
    public void onDeactivateSSResponse(DeactivateSSResponse response) {

    }

    @Override
    public void onInterrogateSSRequest(InterrogateSSRequest request) {

    }

    @Override
    public void onInterrogateSSResponse(InterrogateSSResponse response) {

    }

    @Override
    public void onGetPasswordRequest(GetPasswordRequest request) {

    }

    @Override
    public void onGetPasswordResponse(GetPasswordResponse response) {

    }

    @Override
    public void onRegisterPasswordRequest(RegisterPasswordRequest request) {

    }

    @Override
    public void onRegisterPasswordResponse(RegisterPasswordResponse response) {

    }

    @Override
    public void onProcessUnstructuredSSRequest(ProcessUnstructuredSSRequest procUnstrReqInd) {

    }

    @Override
    public void onProcessUnstructuredSSResponse(ProcessUnstructuredSSResponse procUnstrResInd) {

    }

    @Override
    public void onUnstructuredSSRequest(UnstructuredSSRequest unstrReqInd) {

    }

    @Override
    public void onUnstructuredSSResponse(UnstructuredSSResponse unstrResInd) {

    }

    @Override
    public void onUnstructuredSSNotifyRequest(UnstructuredSSNotifyRequest unstrNotifyInd) {

    }

    @Override
    public void onUnstructuredSSNotifyResponse(UnstructuredSSNotifyResponse unstrNotifyInd) {

    }

    public void performUpdateLocation() {
    }

    public void performPurgeMS() {
    }

    private class HostMessageData {
        public MtMessageData mtMessageData;
        public ResendMessageData resendMessageData;
    }

    private class MtMessageData {
        public String msg;
        public String origIsdnNumber;
        public String vlrNum;
        public String destImsi;
    }

    private class ResendMessageData {
        public SM_RP_DA da;
        public SM_RP_OA oa;
        public SmsSignalInfo si;
        public String msg;
        public String destImsi;
        public String vlrNumber;
        public String origIsdnNumber;
        public String serviceCentreAddr;
    }

    @Override
    public void onReadyForSMRequest(ReadyForSMRequest request) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onReadyForSMResponse(ReadyForSMResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onNoteSubscriberPresentRequest(NoteSubscriberPresentRequest request) {
        // TODO Auto-generated method stub

    }
}
