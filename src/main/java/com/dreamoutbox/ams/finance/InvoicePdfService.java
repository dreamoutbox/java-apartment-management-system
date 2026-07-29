package com.dreamoutbox.ams.finance;

import lombok.RequiredArgsConstructor;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private static final Color BLACK = Color.BLACK;
    private static final Color WHITE = Color.WHITE;
    private static final Color MUTED = new Color(100, 116, 139); // slate gray
    private static final Color LIGHT_BG = new Color(241, 245, 249); // light slate gray for table headers
    private static final Color BORDER = Color.BLACK;

    private Font bold(float size, Color c) {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, size, c);
    }

    private Font regular(float size, Color c) {
        return FontFactory.getFont(FontFactory.HELVETICA, size, c);
    }

    private Font italic(float size, Color c) {
        return FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, size, c);
    }

    public byte[] generate(Invoice invoice,
            BigDecimal elecUsage, BigDecimal waterUsage,
            BigDecimal elecRate, BigDecimal waterRate) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // 1. Header
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[] { 1.2f, 1f });

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(0);
            leftCell.setBackgroundColor(WHITE);
            leftCell.setPadding(0);
            leftCell.addElement(new Paragraph("Apartment Hub", bold(18, BLACK)));
            leftCell.addElement(new Paragraph("Residential Property Management", regular(9, MUTED)));
            header.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(0);
            rightCell.setBackgroundColor(WHITE);
            rightCell.setPadding(0);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(para("INVOICE STATEMENT", bold(14, BLACK), Element.ALIGN_RIGHT));
            rightCell.addElement(
                    para("Invoice #: " + invoice.getInvoiceNumber(), regular(8, MUTED), Element.ALIGN_RIGHT));
            rightCell.addElement(para("Billing Period: " + invoice.getPeriod(), bold(9, BLACK), Element.ALIGN_RIGHT));
            rightCell.addElement(para("Status: " + invoice.getStatus(), bold(9, BLACK), Element.ALIGN_RIGHT));
            header.addCell(rightCell);
            doc.add(header);

            doc.add(spacer(2));
            addHRule(doc, BLACK);

            // 2. Info
            doc.add(spacer(3));
            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);

            String tenantName = safeGet(invoice, "tenantName");
            String tenantPhone = safeGet(invoice, "tenantPhone");
            String tenantEmail = safeGet(invoice, "tenantEmail");
            String roomNum = safeGet(invoice, "roomNumber");
            String floor = safeGet(invoice, "floor");
            String dueDate = invoice.getDueDate() != null ? invoice.getDueDate().toString() : "N/A";

            info.addCell(infoBlock("BILLED TO",
                    new String[] { tenantName, "Phone: " + tenantPhone, "Email: " + tenantEmail }));
            info.addCell(infoBlock("LEASE UNIT DETAILS",
                    new String[] { "Room #: " + roomNum, "Floor: " + floor, "Payment Due Date: " + dueDate }));
            doc.add(info);
            doc.add(spacer(2));

            // 3. Line Items
            addSectionLabel(doc, "Invoice Line Items");
            PdfPTable items = new PdfPTable(2);
            items.setWidthPercentage(100);
            items.setWidths(new float[] { 3f, 1.2f });
            items.addCell(headerCell("DESCRIPTION"));
            items.addCell(headerCell("AMOUNT", Element.ALIGN_RIGHT));

            items.addCell(itemCell("Monthly Room Base Rent"));
            items.addCell(amountCell("$" + invoice.getRentAmount()));

            items.addCell(itemCellNote("Electricity Usage Charge",
                    safe(elecUsage, "0") + " kWh @ $" + safe(elecRate, "0") + "/kWh"));
            items.addCell(amountCell("$" + invoice.getElectricityAmount()));

            items.addCell(itemCellNote("Water Usage Charge",
                    safe(waterUsage, "0") + " m\u00b3 @ $" + safe(waterRate, "0") + "/m\u00b3"));
            items.addCell(amountCell("$" + invoice.getWaterAmount()));

            doc.add(items);
            doc.add(spacer(10));

            // 4. Totals
            BigDecimal paid = invoice.getTotalAmount().subtract(invoice.getBalanceRemaining());

            PdfPTable totals = new PdfPTable(2);
            totals.setWidthPercentage(30);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.setWidths(new float[] { 1.8f, 1f });

            totals.addCell(totalLabelCell("Total Billed Amount:"));
            totals.addCell(totalValueCell("$" + invoice.getTotalAmount(), BLACK));
            totals.addCell(totalLabelCell("Total Paid / Credited:"));
            totals.addCell(totalValueCell("$" + paid, BLACK));
            totals.addCell(totalLabelBold("Balance Remaining:"));
            totals.addCell(totalValueBold("$" + invoice.getBalanceRemaining(), BLACK));
            doc.add(totals);
            doc.add(spacer(10));

            // 5. Footer
            addHRule(doc, BORDER);
            doc.add(spacer(6));
            Paragraph footer = new Paragraph(
                    "Thank you for residing at Apartment Hub. Please ensure prompt payment prior to the due date to avoid late fees.",
                    italic(9, MUTED));
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    private String safeGet(Invoice inv, String field) {
        try {
            return switch (field) {
                case "tenantName" -> inv.getContract().getTenant().getFullName();
                case "tenantPhone" -> {
                    String p = inv.getContract().getTenant().getPhone();
                    yield p != null ? p : "N/A";
                }
                case "tenantEmail" -> {
                    var u = inv.getContract().getTenant().getUser();
                    yield u != null ? u.getUsername() : "N/A";
                }
                case "roomNumber" -> inv.getContract().getRoom().getRoomNumber();
                case "floor" -> String.valueOf(inv.getContract().getRoom().getFloor());
                default -> "N/A";
            };
        } catch (Exception e) {
            return "N/A";
        }
    }

    private String safe(BigDecimal v, String fallback) {
        return v != null ? v.toPlainString() : fallback;
    }

    private Paragraph para(String text, Font font, int align) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(align);
        return p;
    }

    private Paragraph spacer(float h) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(h);
        return p;
    }

    private void addHRule(Document doc, Color color) throws Exception {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        line.setSpacingAfter(8);
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(color);
        cell.setBorderWidthBottom(1.5f);
        cell.setBorderWidthTop(0);
        cell.setBorderWidthLeft(0);
        cell.setBorderWidthRight(0);
        cell.setFixedHeight(2);
        line.addCell(cell);
        doc.add(line);
    }

    private void addSectionLabel(Document doc, String label) throws Exception {
        Paragraph p = new Paragraph(label, bold(10, BLACK));
        p.setSpacingAfter(4);
        doc.add(p);
    }

    private PdfPCell headerCell(String text) {
        return headerCell(text, Element.ALIGN_LEFT);
    }

    private PdfPCell headerCell(String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, bold(9, BLACK)));
        c.setBackgroundColor(LIGHT_BG);
        c.setBorderColor(BORDER);
        c.setPadding(7);
        c.setHorizontalAlignment(align);
        return c;
    }

    private PdfPCell itemCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, regular(9, BLACK)));
        c.setBackgroundColor(WHITE);
        c.setBorderColor(BORDER);
        c.setPadding(7);
        return c;
    }

    private PdfPCell itemCellNote(String title, String note) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(WHITE);
        c.setBorderColor(BORDER);
        c.setPadding(7);
        c.addElement(new Paragraph(title, regular(9, BLACK)));
        c.addElement(new Paragraph(note, regular(8, MUTED)));
        return c;
    }

    private PdfPCell amountCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, regular(9, BLACK)));
        c.setBackgroundColor(WHITE);
        c.setBorderColor(BORDER);
        c.setPadding(7);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private PdfPCell infoBlock(String label, String[] lines) {
        PdfPCell c = new PdfPCell();
        c.setBorder(0);
        c.setPaddingBottom(6);
        c.addElement(new Paragraph(label, bold(8, MUTED)));
        for (int i = 0; i < lines.length; i++) {
            Font f = (i == 0) ? bold(11, BLACK) : regular(9, MUTED);
            c.addElement(new Paragraph(lines[i], f));
        }
        return c;
    }

    private PdfPCell totalLabelCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, regular(9, MUTED)));
        c.setBorder(0);
        c.setPadding(3);
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        return c;
    }

    private PdfPCell totalValueCell(String text, Color color) {
        PdfPCell c = new PdfPCell(new Phrase(text, bold(9, color)));
        c.setBorder(0);
        c.setPadding(3);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private PdfPCell totalLabelBold(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, bold(10, BLACK)));
        c.setBorderColor(BORDER);
        c.setBorderWidthTop(1f);
        c.setBorderWidthBottom(0);
        c.setBorderWidthLeft(0);
        c.setBorderWidthRight(0);
        c.setPadding(5);
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        return c;
    }

    private PdfPCell totalValueBold(String text, Color color) {
        PdfPCell c = new PdfPCell(new Phrase(text, bold(11, color)));
        c.setBorderColor(BORDER);
        c.setBorderWidthTop(1f);
        c.setBorderWidthBottom(0);
        c.setBorderWidthLeft(0);
        c.setBorderWidthRight(0);
        c.setPadding(5);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }
}
