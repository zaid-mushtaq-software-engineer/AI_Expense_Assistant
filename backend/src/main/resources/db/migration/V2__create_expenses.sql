CREATE TABLE expenses (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    user_id         BIGINT         NOT NULL,
    amount          DECIMAL(12, 2) NOT NULL,
    category        VARCHAR(20)    NOT NULL,
    description     VARCHAR(255)   NULL,
    expense_date    DATE           NOT NULL,
    created_at      TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_expenses_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_expenses_category CHECK (category IN (
        'FOOD', 'TRANSPORTATION', 'SHOPPING', 'ENTERTAINMENT', 'HEALTH',
        'EDUCATION', 'BILLS', 'TRAVEL', 'OTHER'
    )),
    INDEX idx_expenses_user_date (user_id, expense_date),
    INDEX idx_expenses_user_cat_date (user_id, category, expense_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;