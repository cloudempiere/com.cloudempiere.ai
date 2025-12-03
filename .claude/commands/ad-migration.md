# Application Dictionary Migration Script Generator

You help create iDempiere Application Dictionary (AD) migration scripts following Cloudempiere standards.

## First Step: ALWAYS Get Ticket Number

**Before creating ANY migration script, you MUST ask for the ticket number if not provided.**

If `$ARGUMENTS` does not include a ticket number (e.g., CLD-XXXX, IDEMPIERE-XXXX):
1. Ask: "What is the ticket number for this migration? (e.g., CLD-1234 or IDEMPIERE-5678)"
2. Wait for the user's response before proceeding

If `$ARGUMENTS` is empty or unclear:
1. Ask for both the ticket number AND what AD changes are needed

## Naming Convention

**Strict format:** `YYYYMMDDHHMI_TICKET-ID.sql`

Examples:
- `202512031430_CLD-1601.sql`
- `202512031445_IDEMPIERE-5678.sql`

**Important:** Do NOT add any description suffix after the ticket number.

## File Locations

Create files in:
- **PostgreSQL:** `migration/postgresql/` or `migration/local_sql/postgresql/`
- **Oracle:** `migration/oracle/` or `migration/local_sql/oracle/`

Always check existing migration folder structure first and follow the project's convention.

## Supported AD Operations

### 1. AD Element (`/ad-migration element`)
```sql
-- CLD-XXXX - Add AD Element for [ColumnName]
SELECT register_migration_script('YYYYMMDDHHMI_CLD-XXXX.sql') FROM dual;

INSERT INTO AD_Element (
    AD_Element_ID, AD_Element_UU,
    AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    ColumnName, EntityType, Name, PrintName, Description, Help
) SELECT
    NEXTVAL('ad_element_seq'), gen_random_uuid(),  -- PG
    -- AD_Element_Seq.NextVal, AD_Element_get_UUID(),  -- Oracle
    0, 0, 'Y', NOW(), 100, NOW(), 100,  -- NOW() for PG, SYSDATE for Oracle
    'ColumnName', 'U', 'Element Name', 'Print Name', 'Description', 'Help text'
WHERE NOT EXISTS (SELECT 1 FROM AD_Element WHERE ColumnName = 'ColumnName');
```

### 2. AD Table (`/ad-migration table`)
```sql
-- CLD-XXXX - Register AD_Table for [TableName]
INSERT INTO AD_Table (
    AD_Table_ID, AD_Table_UU,
    AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Name, TableName, Description,
    AccessLevel, EntityType, IsDeleteable, IsHighVolume, IsSecurityEnabled,
    IsChangeLog, IsView, ReplicationType, CopyColumnsFromTable
) SELECT
    NEXTVAL('ad_table_seq'), gen_random_uuid(),
    0, 0, 'Y', NOW(), 100, NOW(), 100,
    'Table Name', 'CLD_TableName', 'Description',
    '3', 'U', 'Y', 'N', 'N',
    'Y', 'N', 'L', 'N'
WHERE NOT EXISTS (SELECT 1 FROM AD_Table WHERE TableName = 'CLD_TableName');
```

### 3. AD Column (`/ad-migration column`)
```sql
-- CLD-XXXX - Add AD_Column for [TableName].[ColumnName]
INSERT INTO AD_Column (
    AD_Column_ID, AD_Column_UU,
    AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Name, ColumnName, Description,
    AD_Table_ID, AD_Element_ID, AD_Reference_ID,
    FieldLength, IsMandatory, IsKey, IsParent, IsUpdateable, IsIdentifier,
    SeqNo, EntityType, IsSelectionColumn, IsAlwaysUpdateable
) SELECT
    NEXTVAL('ad_column_seq'), gen_random_uuid(),
    0, 0, 'Y', NOW(), 100, NOW(), 100,
    'Column Name', 'ColumnName', 'Description',
    (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'TableName'),
    (SELECT AD_Element_ID FROM AD_Element WHERE ColumnName = 'ColumnName'),
    10,  -- AD_Reference_ID (10=String, 11=Integer, 13=ID, 15=Date, 19=TableDir, 20=YesNo)
    100, 'Y', 'N', 'N', 'Y', 'N',
    0, 'U', 'N', 'N'
WHERE NOT EXISTS (
    SELECT 1 FROM AD_Column c
    JOIN AD_Table t ON c.AD_Table_ID = t.AD_Table_ID
    WHERE t.TableName = 'TableName' AND c.ColumnName = 'ColumnName'
);
```

### 4. AD Field (`/ad-migration field`)
```sql
-- CLD-XXXX - Add AD_Field for [TabName].[ColumnName]
INSERT INTO AD_Field (
    AD_Field_ID, AD_Field_UU,
    AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Name, Description,
    AD_Tab_ID, AD_Column_ID,
    IsDisplayed, IsReadOnly, IsSameLine, IsHeading, IsCentrallyMaintained,
    SeqNo, DisplayLength, EntityType
) SELECT
    NEXTVAL('ad_field_seq'), gen_random_uuid(),
    0, 0, 'Y', NOW(), 100, NOW(), 100,
    'Field Name', 'Description',
    (SELECT AD_Tab_ID FROM AD_Tab WHERE Name = 'TabName'),
    (SELECT AD_Column_ID FROM AD_Column WHERE ColumnName = 'ColumnName'
     AND AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'TableName')),
    'Y', 'N', 'N', 'N', 'Y',
    100, 0, 'U'
WHERE NOT EXISTS (
    SELECT 1 FROM AD_Field f
    JOIN AD_Tab t ON f.AD_Tab_ID = t.AD_Tab_ID
    JOIN AD_Column c ON f.AD_Column_ID = c.AD_Column_ID
    WHERE t.Name = 'TabName' AND c.ColumnName = 'ColumnName'
);
```

### 5. AD Process (`/ad-migration process`)
```sql
-- CLD-XXXX - Register Process [ProcessName]
INSERT INTO AD_Process (
    AD_Process_ID, AD_Process_UU,
    AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Name, Value, Description,
    AccessLevel, EntityType, Classname,
    IsReport, IsDirectPrint, IsBetaFunctionality, IsServerProcess,
    Statistic_Count, Statistic_Seconds, ShowHelp
) SELECT
    NEXTVAL('ad_process_seq'), gen_random_uuid(),
    0, 0, 'Y', NOW(), 100, NOW(), 100,
    'Process Name', 'ProcessValue', 'Description',
    '3', 'U', 'com.cloudempiere.process.ClassName',
    'N', 'N', 'N', 'N',
    0, 0, 'Y'
WHERE NOT EXISTS (SELECT 1 FROM AD_Process WHERE Value = 'ProcessValue');
```

### 6. AD Process Parameter (`/ad-migration param`)
```sql
-- CLD-XXXX - Add Process Parameter [ParameterName]
INSERT INTO AD_Process_Para (
    AD_Process_Para_ID, AD_Process_Para_UU,
    AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Name, ColumnName, Description,
    AD_Process_ID, SeqNo, AD_Reference_ID,
    FieldLength, IsMandatory, IsRange, DefaultValue, EntityType
) SELECT
    NEXTVAL('ad_process_para_seq'), gen_random_uuid(),
    0, 0, 'Y', NOW(), 100, NOW(), 100,
    'Parameter Name', 'ParameterColumn', 'Description',
    (SELECT AD_Process_ID FROM AD_Process WHERE Value = 'ProcessValue'),
    10, 19,  -- SeqNo, AD_Reference_ID
    22, 'Y', 'N', NULL, 'U'
WHERE NOT EXISTS (
    SELECT 1 FROM AD_Process_Para pp
    JOIN AD_Process p ON pp.AD_Process_ID = p.AD_Process_ID
    WHERE p.Value = 'ProcessValue' AND pp.ColumnName = 'ParameterColumn'
);
```

### 7. AD Window/Tab (`/ad-migration window`, `/ad-migration tab`)
Use similar patterns with AD_Window and AD_Tab tables.

### 8. AD Reference List (`/ad-migration reflist`)
```sql
-- CLD-XXXX - Add Reference List values
INSERT INTO AD_Ref_List (
    AD_Ref_List_ID, AD_Ref_List_UU,
    AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Name, Value, Description,
    AD_Reference_ID, EntityType
) SELECT
    NEXTVAL('ad_ref_list_seq'), gen_random_uuid(),
    0, 0, 'Y', NOW(), 100, NOW(), 100,
    'List Value Name', 'VALUE', 'Description',
    (SELECT AD_Reference_ID FROM AD_Reference WHERE Name = 'ReferenceName'), 'U'
WHERE NOT EXISTS (
    SELECT 1 FROM AD_Ref_List rl
    JOIN AD_Reference r ON rl.AD_Reference_ID = r.AD_Reference_ID
    WHERE r.Name = 'ReferenceName' AND rl.Value = 'VALUE'
);
```

## Common Reference Types (AD_Reference_ID)

| ID | Type | Description |
|----|------|-------------|
| 10 | String | Text field |
| 11 | Integer | Whole number |
| 12 | Amount | Currency amount |
| 13 | ID | Primary key |
| 14 | Text | Long text/memo |
| 15 | Date | Date only |
| 16 | DateTime | Date with time |
| 17 | List | Reference list (needs AD_Reference_Value_ID) |
| 19 | TableDir | Foreign key lookup |
| 20 | YesNo | Boolean checkbox |
| 22 | Number | Decimal number |
| 29 | Quantity | Quantity field |
| 30 | Table | Table lookup (needs AD_Reference_Value_ID) |
| 35 | Memo | Large text |

## Access Levels

| Value | Description |
|-------|-------------|
| 1 | Organization |
| 3 | Client+Organization (most common) |
| 4 | System only |
| 6 | System+Client |
| 7 | All |

## Database Syntax Differences

| Feature | PostgreSQL | Oracle |
|---------|------------|--------|
| UUID | `gen_random_uuid()` | `AD_Element_get_UUID()` |
| Sequence | `NEXTVAL('seq_name')` | `Seq_Name.NextVal` |
| Timestamp | `NOW()` | `SYSDATE` |
| Setup | (none needed) | `SET SQLBLANKLINES ON` / `SET DEFINE OFF` |

## Workflow

1. **Ask for ticket** if not provided
2. **Determine AD type** from user request
3. **Generate PostgreSQL script** with proper naming
4. **Generate Oracle script** with syntax adjustments
5. **Place in correct folder** based on project structure
6. **Show summary** of created files
7. **Offer to rename** if ticket number changes

## Post-Creation Options

After creating scripts, ask:
- "Do you want to rename the scripts with a different ticket number?"
- "Should I apply these scripts using RUN_SyncDBDev.sh?"

---

**User's request:** $ARGUMENTS
