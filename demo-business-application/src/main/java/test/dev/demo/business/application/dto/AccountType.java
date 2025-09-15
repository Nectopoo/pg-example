package test.dev.demo.business.application.dto;

public enum AccountType {
    DOLLAR(0),
    RUBLE(1),
    EURO(2);

    private final int code;

    AccountType(final int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static AccountType fromCode(final int code) {
        for (AccountType accountType : values()) {
            if (accountType.code == code) {
                return accountType;
            }
        }
        return null;
    }
}
