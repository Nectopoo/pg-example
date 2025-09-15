package test.dev.smartreplication.example.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "rrko_ru_payment", schema = "schema1")
//@TypeDef(name = "json", typeClass = JsonType.class)
public class RuPayment {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rupay_id_seq")
    @SequenceGenerator(name = "rupay_id_seq", sequenceName = "rupay_id_seq")
    private Long id;

    @JoinColumn(name = "edoc_ref_id", nullable = false)
    @OneToOne(
        targetEntity = EdocRef.class,
        optional = false,
        cascade = {CascadeType.ALL}
    )
    private EdocRef edocRef = new EdocRef(this.getClass().getName());;

    @Column(
        name = "edoc_ref_id",
        insertable = false,
        updatable = false
    )
    private Long edocRefId;

    @Column(name = "archive_date")
    private OffsetDateTime archiveDate;

    @Column(name = "branch_ext_id")
    private Long branchExtId;

    @Column(name = "channel")
    private Integer channel;

    @Column(name = "client_ext_id")
    private Long clientExtId;

    @Column(name = "create_date")
    private OffsetDateTime createDate;

    @Column(name = "delete_date")
    private OffsetDateTime deleteDate;

    @Size(max = 255)
    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "last_modify_date")
    private OffsetDateTime lastModifyDate;

    @Column(name = "note_from_bank_author")
    private Long noteFromBankAuthor;

    @Column(name = "note_from_bank_comment")
    @Type(type = "org.hibernate.type.TextType")
    private String noteFromBankComment;

    @Column(name = "note_from_bank_create_date")
    private OffsetDateTime noteFromBankCreateDate;

    @Size(max = 255)
    @Column(name = "note_from_bank_fio")
    private String noteFromBankFio;

    @Size(max = 255)
    @Column(name = "note_from_receiver")
    private String noteFromReceiver;

    @Column(name = "receive_date")
    private OffsetDateTime receiveDate;

    @Column(name = "send_date")
    private OffsetDateTime sendDate;

    @Size(max = 255)
    @Column(name = "status_action")
    private String statusAction;

    @Size(max = 255)
    @Column(name = "status_base")
    private String statusBase;

    @Size(max = 300)
    @Column(name = "status_comment", length = 300)
    private String statusComment;

    @Column(name = "type_id")
    private Long typeId;

    @Size(max = 38)
    @Column(name = "last_modify_user_ext_id", length = 38)
    private String lastModifyUserExtId;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "authorization_attempt")
    private Integer authorizationAttempt;

    @Size(max = 255)
    @Column(name = "authorization_information")
    private String authorizationInformation;

    @Column(name = "budget_type")
    private Integer budgetType;

    @Size(max = 255)
    @Column(name = "budget_customs_code")
    private String budgetCustomsCode;

    @Size(max = 255)
    @Column(name = "budget_kbk")
    private String budgetKbk;

    @Size(max = 255)
    @Column(name = "budget_oktmo")
    private String budgetOktmo;

    @Size(max = 255)
    @Column(name = "budget_payer_status")
    private String budgetPayerStatus;

    @Size(max = 255)
    @Column(name = "budget_reason_code")
    private String budgetReasonCode;

    @Size(max = 255)
    @Column(name = "budget_reason_document_date")
    private String budgetReasonDocumentDate;

    @Size(max = 255)
    @Column(name = "budget_reason_document_number")
    private String budgetReasonDocumentNumber;

    @Size(max = 255)
    @Column(name = "budget_tax_period_code")
    private String budgetTaxPeriodCode;

    @Column(name = "commission", precision = 19, scale = 2)
    private BigDecimal commission;

    @Size(max = 255)
    @Column(name = "commission_type")
    private String commissionType;

    @Column(name = "debit_date")
    private OffsetDateTime debitDate;

    @Column(name = "document_date")
    private OffsetDateTime documentDate;

    @Column(name = "expected_execution_date")
    private OffsetDateTime expectedExecutionDate;

    @Column(name = "fraud_comment")
    @Type(type = "org.hibernate.type.TextType")
    private String fraudComment;

    @Size(max = 255)
    @Column(name = "fraud_external_ip_address")
    private String fraudExternalIpAddress;

    @Size(max = 255)
    @Column(name = "fraud_internal_ip_address")
    private String fraudInternalIpAddress;

    @Size(max = 255)
    @Column(name = "fraud_mac_address")
    private String fraudMacAddress;

    @Size(max = 255)
    @Column(name = "fraud_state")
    private String fraudState;

    @Column(name = "import_id")
    private Long importId;

    @Size(max = 255)
    @Column(name = "operation_type")
    private String operationType;

    @Size(max = 255)
    @Column(name = "payment_code")
    private String paymentCode;

    @Size(max = 255)
    @Column(name = "payment_ground_description")
    private String paymentGroundDescription;

    @Column(name = "payment_ground_nds", precision = 19, scale = 2)
    private BigDecimal paymentGroundNds;

    @Size(max = 255)
    @Column(name = "payment_ground_nds_calculation")
    private String paymentGroundNdsCalculation;

    @Size(max = 255)
    @Column(name = "payment_ground_nds_percent")
    private String paymentGroundNdsPercent;

    @Size(max = 255)
    @Column(name = "payment_ground_operation_code")
    private String paymentGroundOperationCode;

    @Column(name = "payment_priority")
    private Integer paymentPriority;

    @Size(max = 255)
    @Column(name = "payment_type")
    private String paymentType;

    @Column(name = "payment_type_code")
    private Integer paymentTypeCode;

    @Size(max = 255)
    @Column(name = "recipient_official")
    private String recipientOfficial;

    @Size(max = 255)
    @Column(name = "sender_official")
    private String senderOfficial;

    @Size(max = 255)
    @Column(name = "uin_uip")
    private String uinUip;

    @Size(max = 255)
    @Column(name = "payer_account")
    private String payerAccount;

    @Size(max = 255)
    @Column(name = "payer_additional_information")
    private String payerAdditionalInformation;

    @Size(max = 255)
    @Column(name = "payer_address")
    private String payerAddress;

    @Size(max = 255)
    @Column(name = "payer_inn_kio")
    private String payerInnKio;

    @Size(max = 255)
    @Column(name = "payer_kpp")
    private String payerKpp;

    @Column(name = "payer_name")
    @Type(type = "org.hibernate.type.TextType")
    private String payerName;

    @Size(max = 255)
    @Column(name = "payer_bank_address")
    private String payerBankAddress;

    @Size(max = 255)
    @Column(name = "payer_bank_bic")
    private String payerBankBic;

    @Size(max = 255)
    @Column(name = "payer_bank_corr_account")
    private String payerBankCorrAccount;

    @Size(max = 255)
    @Column(name = "payer_bank_name")
    private String payerBankName;

    @Size(max = 255)
    @Column(name = "receiver_account")
    private String receiverAccount;

    @Size(max = 255)
    @Column(name = "receiver_inn_kio")
    private String receiverInnKio;

    @Size(max = 255)
    @Column(name = "receiver_kpp")
    private String receiverKpp;

    @Column(name = "receiver_name")
    @Type(type = "org.hibernate.type.TextType")
    private String receiverName;

    @Size(max = 255)
    @Column(name = "receiver_bank_address")
    private String receiverBankAddress;

    @Size(max = 255)
    @Column(name = "receiver_bank_bic")
    private String receiverBankBic;

    @Size(max = 255)
    @Column(name = "receiver_bank_corr_account")
    private String receiverBankCorrAccount;

    @Size(max = 255)
    @Column(name = "receiver_bank_name")
    private String receiverBankName;

    @Column(name = "exporting_to_abs")
    private Boolean exportingToAbs;

    @Size(max = 50)
    @Column(name = "cabs_doc_ref", length = 50)
    private String cabsDocRef;

    @Size(max = 50)
    @Column(name = "cabs_doc_ref_ext", length = 50)
    private String cabsDocRefExt;

    @Size(max = 50)
    @Column(name = "fraud_doc_ref_ext", length = 50)
    private String fraudDocRefExt;

    @Size(max = 256)
    @Column(name = "fraud_http_accept", length = 256)
    private String fraudHttpAccept;

    @Size(max = 256)
    @Column(name = "fraud_http_accept_chars", length = 256)
    private String fraudHttpAcceptChars;

    @Size(max = 256)
    @Column(name = "fraud_http_accept_encoding", length = 256)
    private String fraudHttpAcceptEncoding;

    @Size(max = 256)
    @Column(name = "fraud_http_accept_language", length = 256)
    private String fraudHttpAcceptLanguage;

    @Size(max = 256)
    @Column(name = "fraud_http_referer", length = 256)
    private String fraudHttpReferer;

    @Size(max = 256)
    @Column(name = "fraud_user_agent", length = 256)
    private String fraudUserAgent;

    @Size(max = 4096)
    @Column(name = "fraud_device_print", length = 4096)
    private String fraudDevicePrint;

    @Column(name = "cut_off_time_extend_bic")
    private Boolean cutOffTimeExtendBic;

    @Column(name = "cut_off_time_extend_account")
    private Boolean cutOffTimeExtendAccount;

    @Column(name = "optlock")
    private Integer optlock;

    @Size(max = 255)
    @Column(name = "fraud_user_id")
    private String fraudUserId;

    @Column(name = "document_number_as_num")
    private Long documentNumberAsNum;

    @Column(name = "read")
    private Boolean read;

    @Column(name = "import_date")
    private OffsetDateTime importDate;

    @Size(max = 255)
    @Column(name = "res_field_23")
    private String resField23;

    @Column(name = "import_session_id")
    private Long importSessionId;

    @Column(name = "source_id")
    private Long sourceId;

    @Size(max = 255)
    @Column(name = "fraud_user_login")
    private String fraudUserLogin;

    @Size(max = 1)
    @Column(name = "loan_accept", length = 1)
    private String loanAccept;

    @Column(name = "note_bank_employee_author")
    private Long noteBankEmployeeAuthor;

    @Column(name = "note_bank_employee_comment")
    @Type(type = "org.hibernate.type.TextType")
    private String noteBankEmployeeComment;

    @Column(name = "note_bank_employee_create_date")
    private OffsetDateTime noteBankEmployeeCreateDate;

    @Size(max = 255)
    @Column(name = "note_bank_employee_fio")
    private String noteBankEmployeeFio;

    @Column(name = "note_fraud_author")
    private Long noteFraudAuthor;

    @Column(name = "note_fraud_comment")
    @Type(type = "org.hibernate.type.TextType")
    private String noteFraudComment;

    @Column(name = "note_fraud_create_date")
    private OffsetDateTime noteFraudCreateDate;

    @Size(max = 255)
    @Column(name = "note_fraud_fio")
    private String noteFraudFio;

    @Size(max = 4000)
    @Column(name = "message_for_bank", length = 4000)
    private String messageForBank;

    @Size(max = 50)
    @Column(name = "message_for_bank_code", length = 50)
    private String messageForBankCode;

    @Column(name = "rates_confirmed")
    private Boolean ratesConfirmed;

    @Column(name = "cold_data")
    private Boolean coldData;

    @Column(name = "budget_ext_id")
    private Long budgetExtId;

    @Size(max = 255)
    @Column(name = "budget_name")
    private String budgetName;

    @Size(max = 64)
    @Column(name = "kesr_code", length = 64)
    private String kesrCode;

    @Size(max = 1024)
    @Column(name = "kesr_code_name", length = 1024)
    private String kesrCodeName;

    @Column(name = "is_exported")
    private Boolean isExported;

    @Size(max = 35)
    @Column(name = "income_type_code", length = 35)
    private String incomeTypeCode;

    @Column(name = "recovery_amount", precision = 19, scale = 2)
    private BigDecimal recoveryAmount;

    @Size(max = 30)
    @Column(name = "sign_bsk", length = 30)
    private String signBsk;

    @Size(max = 50)
    @Column(name = "uuid", length = 50)
    private String uuid;

    @Column(name = "value_date")
    private OffsetDateTime valueDate;

    @Size(max = 255)
    @Column(name = "abs_type_ext")
    private String absTypeExt;

    @Column(name = "manual_acceptance_end_date")
    private OffsetDateTime manualAcceptanceEndDate;

    @Size(max = 255)
    @Column(name = "manual_acceptance_official")
    private String manualAcceptanceOfficial;

    @Column(name = "registry_ref_doc_id")
    private Long registryRefDocId;

    @Column(name = "registry_date")
    private OffsetDateTime registryDate;

    @Size(max = 255)
    @Column(name = "registry_number")
    private String registryNumber;

    @Size(max = 38)
    @Column(name = "edk_doc_ref_ext", length = 38)
    private String edkDocRefExt;

    @Size(max = 40)
    @Column(name = "credit_register", length = 40)
    private String creditRegister;

    @Size(max = 255)
    @Column(name = "role_template_signature")
    private String roleTemplateSignature;

    @Column(name = "manual_accept_results")
    private Short manualAcceptResults;

    @Size(max = 255)
    @Column(name = "registry_payer_account")
    private String registryPayerAccount;

    @Size(max = 255)
    @Column(name = "registry_bic")
    private String registryBic;

    @Size(max = 20)
    @Column(name = "create_type", length = 20)
    private String createType;

    @Size(max = 255)
    @Column(name = "payment_type_int")
    private String paymentTypeInt;

    @Size(max = 255)
    @Column(name = "budget_reason_code_int")
    private String budgetReasonCodeInt;

    @Column(name = "migrated")
    private Boolean migrated;

    @Column(name = "registry_client_id")
    private Long registryClientId;

    @Size(max = 255)
    @Column(name = "payer_name_int")
    private String payerNameInt;

    @Size(max = 255)
    @Column(name = "payer_bank_name_int")
    private String payerBankNameInt;

    @Size(max = 255)
    @Column(name = "payer_address_int")
    private String payerAddressInt;

    @Size(max = 255)
    @Column(name = "receiver_bank_name_int")
    private String receiverBankNameInt;

    @Column(name = "contractor_rule_id")
    private Long contractorRuleId;

    @Column(name = "contractor_rule_version")
    private Integer contractorRuleVersion;

    @Size(max = 255)
    @Column(name = "contractor_zok_number")
    private String contractorZokNumber;

    @Column(name = "contractor_zok_date")
    private LocalDate contractorZokDate;

    @Size(max = 40)
    @Column(name = "contractor_restriction", length = 40)
    private String contractorRestriction;

    @Size(max = 40)
    @Column(name = "signature_session_id", length = 40)
    private String signatureSessionId;

    @Column(name = "contractor_restriction_check_date")
    private LocalDate contractorRestrictionCheckDate;

    @Size(max = 50)
    @Column(name = "source_name", length = 50)
    private String sourceName;

    @Column(name = "fine_edoc_id")
    private Long fineEdocId;

    @Size(max = 50)
    @Column(name = "payment_message_type", length = 50)
    private String paymentMessageType;

    @NotNull
    @Column(name = "visible_for_acceptor", nullable = false)
    private Boolean visibleForAcceptor = false;

    @Size(max = 255)
    @Column(name = "contractor_restriction_check_code")
    private String contractorRestrictionCheckCode;

    @Size(max = 255)
    @Column(name = "contractor_restriction_check_hint")
    private String contractorRestrictionCheckHint;

    @Column(name = "contractor_restriction_check_params")
    private byte[] contractorRestrictionCheckParams;

    @Size(max = 100)
    @Column(name = "localized_status_base", length = 100)
    private String localizedStatusBase;

    @Column(name = "part_document_amount", precision = 19, scale = 2)
    private BigDecimal partDocumentAmount;
    @Size(max = 40)
    @Column(name = "smb_document_id", length = 40)
    private String smbDocumentId;

    @Size(max = 255)
    @Column(name = "mac_address")
    private String macAddress;
    @Column(name = "payer_account_id")
    private Long payerAccountId;

    @Column(name = "contractor_id")
    private Long contractorId;
    @Size(max = 50)
    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "registry_doc_type")
    private Integer registryDocType;
    @Size(max = 200)
    @Column(name = "payer_account_key", length = 200)
    private String payerAccountKey;

    @Column(name = "registry_sign_time")
    private OffsetDateTime registrySignTime;
    @Column(name = "delivered_date")
    private OffsetDateTime deliveredDate;

    @Column(name = "special_accept")
    private Boolean specialAccept;
    @Size(max = 50)
    @Column(name = "receiver_type", length = 50)
    private String receiverType;

    @Column(name = "registry_edoc_ref_id")
    private Long registryEdocRefId;
    @NotNull
    @Column(name = "budget_need_sections", nullable = false)
    private Boolean budgetNeedSections = false;

    @Column(name = "payer_account_ext_id")
    private Long payerAccountExtId;

/*
    TODO [JPA Buddy] create field to map the 'branch_snapshot' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping

 */
//    @Type(type = "json")
//    @Column(name = "branch_snapshot", columnDefinition = "jsonb")
//    private Snapshot branchSnapshot;

/*
    TODO [JPA Buddy] create field to map the 'client_snapshot' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "client_snapshot", columnDefinition = "jsonb(0, 0)")
    private Object clientSnapshot;
*/
/*
    TODO [JPA Buddy] create field to map the 'last_modify_user_snapshot' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "last_modify_user_snapshot", columnDefinition = "jsonb(0, 0)")
    private Object lastModifyUserSnapshot;
*/
/*
    TODO [JPA Buddy] create field to map the 'status_bank_backend_response_message' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "status_bank_backend_response_message", columnDefinition = "jsonb(0, 0)")
    private Object statusBankBackendResponseMessage;
*/
/*
    TODO [JPA Buddy] create field to map the 'status_extended' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "status_extended", columnDefinition = "jsonb(0, 0)")
    private Object statusExtended;
*/
/*
    TODO [JPA Buddy] create field to map the 'supporting_document_ids' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "supporting_document_ids", columnDefinition = "bigint[](0, 0)")
    private Object supportingDocumentIds;
*/
/*
    TODO [JPA Buddy] create field to map the 'budget_control_items' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "budget_control_items", columnDefinition = "jsonb(0, 0)")
    private Object budgetControlItems;
*/
/*
    TODO [JPA Buddy] create field to map the 'part_document_ids' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "part_document_ids", columnDefinition = "jsonb(0, 0)")
    private Object partDocumentIds;
*/
/*
    TODO [JPA Buddy] create field to map the 'params' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "params", columnDefinition = "jsonb(0, 0)")
    private Object params;
*/
/*
    TODO [JPA Buddy] create field to map the 'card_indexes' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "card_indexes", columnDefinition = "jsonb(0, 0)")
    private Object cardIndexes;
*/
/*
    TODO [JPA Buddy] create field to map the 'registry_signatures' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "registry_signatures", columnDefinition = "jsonb(0, 0)")
    private Object registrySignatures;
*/
/*
    TODO [JPA Buddy] create field to map the 'budget_sections' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "budget_sections", columnDefinition = "jsonb(0, 0)")
    private Object budgetSections;
*/
}
