# ADR-023: OCR Invoice Processing Use Case

**Status**: Proposed
**Date**: 2025-12-03
**Deciders**: Cloudempiere AI Team
**Phase**: 3 (Future)
**Related**: [ADR-002](002-langchain4j-strategic-adoption.md), [ADR-009](009-domain-boundaries-agent-scope.md), [MVP Documentation](../iDempiere-AI-Assistant-MVP-Documentation.md)

## Context

### Problem Statement

Invoice processing is manual and error-prone:
- Vendor invoices arrive as PDFs, scans, or images
- Data entry takes 5-15 minutes per invoice
- Human errors in amounts, dates, vendor matching
- No automatic validation against PO or receipt
- Backlog builds up, delaying payments and discounts

### Business Value

| Metric | Current | Target | Impact |
|--------|---------|--------|--------|
| Invoice processing time | 5-15 min | <1 min | 90% reduction |
| Data entry errors | 2-5% | <0.5% | 80% fewer errors |
| Invoice backlog | Days | Hours | Cash flow improvement |
| Early payment discount capture | 40% | 80% | Cost savings |

### Technology Landscape

Modern AI provides multiple approaches:
- **Vision LLMs**: Claude 3.5, GPT-4V can read invoice images directly
- **Specialized OCR**: AWS Textract, Google Document AI
- **Hybrid**: OCR extraction + LLM interpretation

## Decision

Implement an **OCR Invoice Processing** feature using Vision LLM (Claude) with fallback to structured OCR, enabling automatic extraction and matching of vendor invoice data.

### Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                    Invoice Upload                                     │
│  ┌──────────────────────────────────────────────────────────────────┐│
│  │  Drop Zone: PDF, PNG, JPG, TIFF                                  ││
│  │  Email Import: Attachments from vendor emails                    ││
│  │  Batch Upload: Multiple invoices at once                         ││
│  └──────────────────────────────────────────────────────────────────┘│
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    Document Preprocessor                              │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ Processing:                                                     │  │
│  │   - PDF to image conversion (if needed)                         │  │
│  │   - Image quality enhancement                                   │  │
│  │   - Multi-page handling                                         │  │
│  │   - Orientation correction                                      │  │
│  │   - Resolution normalization                                    │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    InvoiceExtractionAgent (Vision LLM)                │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ LangChain4j AiServices with Vision:                             │  │
│  │   - ChatLanguageModel (Claude 3.5 Sonnet with vision)           │  │
│  │   - System prompt: Invoice extraction specialist                │  │
│  │   - Image input: Invoice document                               │  │
│  │   - Output: Structured invoice data                             │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    Extracted Invoice Data                             │
│  {                                                                    │
│    "vendor_name": "ABC Supplies GmbH",                               │
│    "vendor_tax_id": "DE123456789",                                   │
│    "invoice_number": "INV-2025-001234",                              │
│    "invoice_date": "2025-01-15",                                     │
│    "due_date": "2025-02-15",                                         │
│    "currency": "EUR",                                                │
│    "subtotal": 1000.00,                                              │
│    "tax_amount": 190.00,                                             │
│    "total_amount": 1190.00,                                          │
│    "line_items": [...],                                              │
│    "po_reference": "PO-2025-000789",                                 │
│    "confidence": 0.95                                                │
│  }                                                                    │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    InvoiceMatchingAgent                               │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ Matching Logic:                                                 │  │
│  │   - Vendor lookup by name/tax ID                                │  │
│  │   - PO matching by reference                                    │  │
│  │   - Receipt matching (3-way match)                              │  │
│  │   - Price/quantity validation                                   │  │
│  │   - Duplicate detection                                         │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    Validation & Review                                │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ Confidence Routing:                                             │  │
│  │   - HIGH (>0.95): Auto-create draft invoice                     │  │
│  │   - MEDIUM (0.80-0.95): Create with review flag                 │  │
│  │   - LOW (<0.80): Manual review required                         │  │
│  │                                                                  │  │
│  │ Validation Checks:                                              │  │
│  │   - Math verification (lines sum to total)                      │  │
│  │   - Tax calculation check                                       │  │
│  │   - Duplicate invoice detection                                 │  │
│  │   - Vendor credit limit                                         │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    C_Invoice (Draft)                                  │
│  - Vendor invoice auto-populated                                     │
│  - Lines matched to PO/Receipt                                       │
│  - Original PDF attached                                             │
│  - AI extraction metadata stored                                     │
│  - Ready for approval workflow                                       │
└──────────────────────────────────────────────────────────────────────┘
```

### System Prompt (Vision Extraction)

```
You are an expert invoice data extraction system.

TASK: Extract all relevant data from this invoice image.

OUTPUT FORMAT (JSON only):
{
  "vendor": {
    "name": "Full company name",
    "address": "Full address",
    "tax_id": "VAT/Tax ID number",
    "phone": "Phone if visible",
    "email": "Email if visible",
    "bank_account": "IBAN or account number if visible"
  },
  "invoice": {
    "number": "Invoice number/ID",
    "date": "YYYY-MM-DD",
    "due_date": "YYYY-MM-DD",
    "po_reference": "PO number if referenced",
    "delivery_note": "Delivery note number if referenced"
  },
  "amounts": {
    "currency": "EUR/USD/etc",
    "subtotal": 0.00,
    "discount": 0.00,
    "tax_rate": 19.0,
    "tax_amount": 0.00,
    "shipping": 0.00,
    "total": 0.00
  },
  "line_items": [
    {
      "line_number": 1,
      "description": "Item description",
      "sku": "SKU if present",
      "quantity": 0.0,
      "unit": "PCS/KG/etc",
      "unit_price": 0.00,
      "line_total": 0.00,
      "tax_rate": 19.0
    }
  ],
  "payment": {
    "terms": "Net 30/etc",
    "discount_terms": "2% 10 Net 30 if present",
    "payment_method": "Bank transfer/etc"
  },
  "metadata": {
    "language": "de/en/etc",
    "confidence": 0.0-1.0,
    "extraction_notes": ["Note 1", "Note 2"],
    "unclear_fields": ["field1", "field2"]
  }
}

EXTRACTION RULES:

1. Numbers:
   - Use decimal point (1234.56) not comma for amounts
   - Preserve original precision
   - Parse European format (1.234,56) correctly

2. Dates:
   - Convert all dates to YYYY-MM-DD format
   - Handle European (DD.MM.YYYY) and US (MM/DD/YYYY) formats

3. Tax:
   - Identify tax rate from invoice
   - Calculate expected tax if not shown
   - Flag if calculation doesn't match

4. Line Items:
   - Extract all visible line items
   - Match quantity × price = line total
   - Flag any math discrepancies

5. References:
   - Look for PO numbers (PO-, Purchase Order, Bestellung)
   - Look for delivery notes (DN-, Lieferschein)
   - Look for contract references

6. Confidence:
   - 0.95+: All fields clear and consistent
   - 0.80-0.94: Minor ambiguity or unclear fields
   - 0.60-0.79: Multiple unclear fields
   - <0.60: Poor quality, manual review needed

7. Languages:
   - German: Rechnung, Rechnungsnummer, MwSt, Netto, Brutto
   - French: Facture, Numéro de facture, TVA, HT, TTC
   - Spanish: Factura, Número de factura, IVA
   - English: Invoice, Invoice Number, Tax, Net, Gross
```

### Agent Definition

```java
package com.cloudempiere.ai.agent.invoice;

@Agent(
    name = "InvoiceExtractionAgent",
    domain = "PURCHASING",
    riskLevel = RiskLevel.MEDIUM
)
public interface InvoiceExtractionAgent {

    @SystemMessage(fromResource = "prompts/invoice-extraction.txt")
    ExtractedInvoice extractFromImage(
        @UserMessage String instructions,
        @V Image invoiceImage
    );

    @SystemMessage(fromResource = "prompts/invoice-extraction.txt")
    List<ExtractedInvoice> extractFromMultiPage(
        @UserMessage String instructions,
        @V List<Image> pages
    );
}

@Agent(
    name = "InvoiceMatchingAgent",
    domain = "PURCHASING",
    riskLevel = RiskLevel.LOW
)
public interface InvoiceMatchingAgent {

    @SystemMessage(fromResource = "prompts/invoice-matching.txt")
    MatchingResult matchInvoice(
        @MemoryId String sessionId,
        @UserMessage String extractedInvoiceJson
    );
}

public record ExtractedInvoice(
    @Description("Vendor information") VendorInfo vendor,
    @Description("Invoice header") InvoiceHeader invoice,
    @Description("Monetary amounts") InvoiceAmounts amounts,
    @Description("Line items") List<InvoiceLineItem> line_items,
    @Description("Payment terms") PaymentInfo payment,
    @Description("Extraction metadata") ExtractionMetadata metadata
) {}

public record VendorInfo(
    String name,
    String address,
    String tax_id,
    String phone,
    String email,
    String bank_account
) {}

public record InvoiceHeader(
    String number,
    LocalDate date,
    LocalDate due_date,
    String po_reference,
    String delivery_note
) {}

public record InvoiceAmounts(
    String currency,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal tax_rate,
    BigDecimal tax_amount,
    BigDecimal shipping,
    BigDecimal total
) {}

public record InvoiceLineItem(
    int line_number,
    String description,
    String sku,
    BigDecimal quantity,
    String unit,
    BigDecimal unit_price,
    BigDecimal line_total,
    BigDecimal tax_rate
) {}

public record MatchingResult(
    @Description("Matched vendor") Integer C_BPartner_ID,
    @Description("Matched PO") Integer C_Order_ID,
    @Description("Matched receipts") List<Integer> M_InOut_IDs,
    @Description("Match confidence") double confidence,
    @Description("Match issues") List<String> issues,
    @Description("Suggested actions") List<String> actions
) {}
```

### Boundaries (per ADR-009)

```yaml
Agent: InvoiceExtractionAgent
Domain: PURCHASING
Risk Level: MEDIUM

Data Access:
  Read Tables:
    - C_BPartner (vendor lookup)
    - C_BPartner_Location (addresses)
    - C_Order (PO matching)
    - C_OrderLine (line matching)
    - M_InOut (receipt matching)
    - M_InOutLine (receipt line matching)
    - M_Product (product lookup)
    - C_Invoice (duplicate detection)
    - C_Currency (currency handling)
    - C_Tax (tax rate lookup)

  Write Tables:
    - C_Invoice (create draft only)
    - C_InvoiceLine (create with invoice)
    - Attachment storage (original PDF)

  Forbidden Tables:
    - C_Payment (payment processing)
    - C_BankAccount (bank details)
    - Pricing/discount tables
    - AD_User credentials

Organizational:
  - Respects AD_Role access
  - Vendor filtered by AD_Org_ID
  - PO/Receipt matching within org

Token Limits:
  Max Tokens/Request: 15,000 (vision + extraction)
  Max Pages/Invoice: 10

Cost:
  Daily Budget: $200
  Cost per Invoice: ~$0.15 (estimated with vision)

Performance:
  Response Time: <10 seconds (single page)
  Batch Throughput: 20 invoices/hour
```

### Invoice Matching Service

```java
public class InvoiceMatchingService {

    /**
     * Match extracted invoice to iDempiere entities
     */
    public MatchingResult match(ExtractedInvoice extracted) {
        MatchingResult result = new MatchingResult();

        // 1. Vendor matching
        MBPartner vendor = matchVendor(extracted.vendor());
        if (vendor != null) {
            result.setC_BPartner_ID(vendor.getC_BPartner_ID());
            result.addConfidence(0.3);
        } else {
            result.addIssue("Vendor not found: " + extracted.vendor().name());
            result.addAction("Create new vendor or select manually");
        }

        // 2. PO matching
        if (extracted.invoice().po_reference() != null) {
            MOrder po = matchPO(extracted.invoice().po_reference(), vendor);
            if (po != null) {
                result.setC_Order_ID(po.getC_Order_ID());
                result.addConfidence(0.3);

                // 3. Line matching
                matchLines(extracted.line_items(), po, result);
            } else {
                result.addIssue("PO not found: " + extracted.invoice().po_reference());
            }
        }

        // 4. Receipt matching (3-way match)
        if (result.getC_Order_ID() != null) {
            List<MInOut> receipts = findMatchingReceipts(result.getC_Order_ID());
            result.setM_InOut_IDs(receipts.stream()
                .map(MInOut::getM_InOut_ID)
                .collect(toList()));
            if (!receipts.isEmpty()) {
                result.addConfidence(0.2);
            }
        }

        // 5. Duplicate detection
        if (isDuplicate(extracted, vendor)) {
            result.addIssue("Possible duplicate invoice");
            result.setConfidence(0.0);  // Force manual review
        }

        // 6. Amount validation
        validateAmounts(extracted, result);

        return result;
    }

    private MBPartner matchVendor(VendorInfo vendorInfo) {
        // Try tax ID first (most reliable)
        if (vendorInfo.tax_id() != null) {
            MBPartner vendor = new Query(ctx, MBPartner.Table_Name,
                "TaxID=? AND IsVendor='Y' AND IsActive='Y'", null)
                .setParameters(vendorInfo.tax_id())
                .first();
            if (vendor != null) return vendor;
        }

        // Try name matching (fuzzy)
        return new Query(ctx, MBPartner.Table_Name,
            "LOWER(Name) LIKE ? AND IsVendor='Y' AND IsActive='Y'", null)
            .setParameters("%" + vendorInfo.name().toLowerCase() + "%")
            .first();
    }

    private boolean isDuplicate(ExtractedInvoice extracted, MBPartner vendor) {
        if (vendor == null) return false;

        return new Query(ctx, MInvoice.Table_Name,
            "C_BPartner_ID=? AND DocumentNo=? AND IsSOTrx='N'", null)
            .setParameters(vendor.getC_BPartner_ID(), extracted.invoice().number())
            .match();
    }
}
```

### Invoice Creator

```java
public class AIInvoiceCreator {

    /**
     * Create draft invoice from extraction
     */
    public MInvoice createDraftInvoice(ExtractedInvoice extracted,
                                        MatchingResult matching) {
        MInvoice invoice = new MInvoice(ctx, 0, trxName);

        // Header
        invoice.setIsSOTrx(false);  // Vendor invoice
        invoice.setC_DocTypeTarget_ID(getAPInvoiceDocType());

        if (matching.getC_BPartner_ID() != null) {
            invoice.setC_BPartner_ID(matching.getC_BPartner_ID());
        }

        invoice.setDocumentNo(extracted.invoice().number());
        invoice.setDateInvoiced(Timestamp.valueOf(
            extracted.invoice().date().atStartOfDay()));
        invoice.setDateAcct(invoice.getDateInvoiced());

        // Currency
        MCurrency currency = MCurrency.get(ctx,
            extracted.amounts().currency());
        if (currency != null) {
            invoice.setC_Currency_ID(currency.getC_Currency_ID());
        }

        // PO reference
        if (matching.getC_Order_ID() != null) {
            invoice.setC_Order_ID(matching.getC_Order_ID());
            invoice.setPOReference(extracted.invoice().po_reference());
        }

        // AI metadata
        invoice.set_ValueOfColumn("AI_Extracted", true);
        invoice.set_ValueOfColumn("AI_Confidence",
            matching.getConfidence());
        invoice.set_ValueOfColumn("AI_ExtractedAt",
            Timestamp.valueOf(LocalDateTime.now()));

        invoice.saveEx();

        // Lines
        for (InvoiceLineItem lineItem : extracted.line_items()) {
            createInvoiceLine(invoice, lineItem, matching);
        }

        // Attach original document
        attachOriginalDocument(invoice);

        // Validate totals
        validateAndFlag(invoice, extracted);

        return invoice;
    }

    private void createInvoiceLine(MInvoice invoice,
                                   InvoiceLineItem lineItem,
                                   MatchingResult matching) {
        MInvoiceLine line = new MInvoiceLine(invoice);

        line.setLine(lineItem.line_number() * 10);
        line.setDescription(lineItem.description());
        line.setQty(lineItem.quantity());
        line.setPriceEntered(lineItem.unit_price());
        line.setPriceActual(lineItem.unit_price());

        // Product matching
        if (lineItem.sku() != null) {
            MProduct product = matchProduct(lineItem.sku());
            if (product != null) {
                line.setM_Product_ID(product.getM_Product_ID());
                line.setC_UOM_ID(product.getC_UOM_ID());
            }
        }

        // Tax
        MTax tax = findTax(lineItem.tax_rate());
        if (tax != null) {
            line.setC_Tax_ID(tax.getC_Tax_ID());
        }

        line.saveEx();
    }
}
```

## Implementation Plan

### Phase 1: Vision Extraction (Days 1-5)

| Task | Effort | Owner |
|------|--------|-------|
| Document preprocessor (PDF→Image) | 8h | Dev |
| LangChain4j vision model integration | 8h | Dev |
| `InvoiceExtractionAgent` with vision | 8h | Dev |
| System prompt for extraction | 6h | Dev |
| Multi-page handling | 6h | Dev |

### Phase 2: Matching & Validation (Days 6-10)

| Task | Effort | Owner |
|------|--------|-------|
| `InvoiceMatchingService` | 8h | Dev |
| Vendor matching (tax ID, name) | 4h | Dev |
| PO/Receipt matching | 8h | Dev |
| Line item matching | 6h | Dev |
| Duplicate detection | 4h | Dev |
| Amount validation | 4h | Dev |

### Phase 3: Invoice Creation (Days 11-14)

| Task | Effort | Owner |
|------|--------|-------|
| `AIInvoiceCreator` | 8h | Dev |
| Draft invoice generation | 6h | Dev |
| Line creation with matching | 6h | Dev |
| Document attachment | 4h | Dev |
| AI metadata storage | 2h | Dev |

### Phase 4: UI & Testing (Days 15-20)

| Task | Effort | Owner |
|------|--------|-------|
| Upload UI (drag-drop, email import) | 8h | Dev |
| Extraction preview/edit screen | 8h | Dev |
| Batch processing UI | 6h | Dev |
| Testing with 100 real invoices | 12h | QA |
| Accuracy measurement | 8h | QA |

### Deliverables

- `InvoiceExtractionAgent.java` (~150 lines)
- `InvoiceMatchingAgent.java` (~100 lines)
- `InvoiceMatchingService.java` (~300 lines)
- `AIInvoiceCreator.java` (~250 lines)
- `DocumentPreprocessor.java` (~200 lines)
- `invoice-extraction.txt` (system prompt)
- `invoice-matching.txt` (system prompt)
- Invoice upload ZK window
- Extraction preview dialog
- Batch processing interface

**Total Effort**: 20 days
**Risk Level**: High (OCR accuracy, matching complexity)

## Acceptance Criteria

```yaml
Functional:
  - [ ] Upload PDF/image invoices
  - [ ] Extract vendor, amounts, lines
  - [ ] Match vendor by tax ID or name
  - [ ] Match PO by reference
  - [ ] Create draft vendor invoice
  - [ ] Attach original document
  - [ ] Batch processing

Non-Functional:
  - [ ] Extraction accuracy: 95%+ (header fields)
  - [ ] Line item accuracy: 90%+
  - [ ] Processing time: <10 seconds/page
  - [ ] Cost per invoice: <$0.20

Matching:
  - [ ] Vendor match rate: 90%+
  - [ ] PO match rate: 85%+ (when referenced)
  - [ ] Duplicate detection: 99%+
```

## Consequences

### Positive

1. **Automation**: 90% reduction in data entry
2. **Accuracy**: Fewer human errors
3. **Speed**: Invoices processed immediately
4. **Matching**: Automatic PO/receipt linkage
5. **Audit Trail**: Original document preserved

### Negative

1. **Vision Cost**: Higher API cost than text
2. **Quality Dependency**: Poor scans = poor extraction
3. **Complexity**: Matching logic is complex
4. **Edge Cases**: Unusual invoice formats

### Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Poor image quality | HIGH | Preprocessing, confidence routing |
| Wrong vendor match | HIGH | Tax ID priority, manual review |
| Duplicate invoices | HIGH | Strong duplicate detection |
| Amount mismatch | MEDIUM | Math validation, flags |

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Extraction accuracy | 95%+ | Sample review |
| Vendor match rate | 90%+ | Match tracking |
| Auto-process rate | 70%+ | Confidence > 0.95 |
| Processing time | <10 sec | Performance logs |
| Cost per invoice | <$0.20 | API billing |
| Error rate | <1% | Post-processing audit |

## References

- [MVP Documentation](../iDempiere-AI-Assistant-MVP-Documentation.md)
- [ADR-002: LangChain4j Strategic Adoption](002-langchain4j-strategic-adoption.md)
- [ADR-009: Domain Boundaries](009-domain-boundaries-agent-scope.md)
- [Claude Vision API](https://docs.anthropic.com/claude/docs/vision)

---

*ADR-023 | Version 1.0 | 2025-12-03*
