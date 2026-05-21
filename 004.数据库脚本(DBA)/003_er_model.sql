-- ============================================================================
-- 每日财务管家 — ER 关系图 (Mermaid)
-- 可在支持 Mermaid 的 Markdown 编辑器中预览
-- ============================================================================

/*

```mermaid
erDiagram
    USER ||--o{ BILL : creates
    USER ||--o{ CATEGORY : creates
    USER ||--o{ USER_THIRD_PARTY_ACCOUNT : binds
    USER ||--o{ BUDGET : sets
    USER ||--o{ BILL_TAG : creates
    CATEGORY ||--o{ CATEGORY : has_sub
    BILL }o--|| CATEGORY : belongs_to
    BILL ||--o{ BILL_TAG_REL : tagged
    BILL_TAG ||--o{ BILL_TAG_REL : applies_to

    USER {
        bigint id PK
        varchar nickname
        varchar avatar_url
        varchar phone UK
        varchar country_code
        varchar password_hash
        varchar currency
        varchar theme
        tinyint status
        datetime created_at
        datetime updated_at
    }

    USER_THIRD_PARTY_ACCOUNT {
        bigint id PK
        bigint user_id FK
        varchar platform
        varchar open_id
        varchar union_id
        varchar nickname
        varchar avatar_url
        datetime created_at
    }

    CATEGORY {
        bigint id PK
        varchar name
        varchar icon
        enum type
        bigint parent_id FK
        int sort_order
        tinyint is_preset
        tinyint is_archived
        bigint user_id
        datetime created_at
    }

    BILL {
        bigint id PK
        bigint user_id
        enum type
        decimal amount
        bigint category_id FK
        varchar remark
        datetime bill_time
        tinyint is_recurring
        bigint created_by
        datetime created_at
        datetime updated_at
    }

    BILL_TAG {
        bigint id PK
        bigint user_id
        varchar name
        datetime created_at
    }

    BILL_TAG_REL {
        bigint id PK
        bigint bill_id FK
        bigint tag_id FK
    }

    BUDGET {
        bigint id PK
        bigint user_id
        bigint category_id
        decimal amount
        enum period
        date start_date
        date end_date
        tinyint is_active
        datetime created_at
        datetime updated_at
    }
```

*/


-- ============================================================================
-- 核心实体关系说明
-- ============================================================================

/*

1. User (用户) 1 ──→ n Bill (账单)
   一个用户可以创建多条账单记录

2. User (用户) 1 ──→ n Category (分类)
   用户可以自定义分类 (user_id > 0)，系统预设分类 (user_id = 0)

3. Category (分类) 1 ──→ n Category (分类)
   一级分类 (parent_id = 0) 可以有多个二级分类 (parent_id > 0)，最多两级

4. Category (分类) 1 ──→ n Bill (账单)
   每条账单必须关联一个分类

5. Bill (账单) n ──→ m BillTag (标签)
   通过 bill_tag_rel 关联表实现多对多关系

6. User (用户) 1 ──→ n Budget (预算)
   用户可以设置多个预算（总预算 + 分类预算）

7. User (用户) 1 ──→ n UserThirdPartyAccount (第三方账号)
   一个用户可以绑定多个第三方登录账号

-- 后续版本预留 (V2.0+)

8. User (用户) n ──→ n SharedBook (共享账本)
   通过 BookMember 关联表实现多对多关系

9. User (用户) 1 ──→ n RecurringBill (周期性账单)
   用户可以创建多条周期性账单模板

*/
