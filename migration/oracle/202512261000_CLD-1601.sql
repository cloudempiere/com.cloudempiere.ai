-- Rename Satellite provider to AI Hub for clarity
SELECT register_migration_script('202512261000_Rename_Satellite_to_AIHub.sql') FROM dual;

-- Update AD_Ref_List name from "Quarkus Satellite" to "iDempiere AI Hub"
UPDATE AD_Ref_List
SET Name = 'iDempiere AI Hub',
    Updated = SYSDATE,
    UpdatedBy = 100
WHERE AD_Reference_ID = 800124
  AND Value = 'SAT';
