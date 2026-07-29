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
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PaymentPdfService {

    private static final Color BLACK = Color.BLACK;
    private static final Color WHITE = Color.WHITE;
    private static final Color MUTED = new Color(100, 116, 139); // slate gray
    private static final Color LIGHT_BG = new Color(241, 245, 249); // light slate gray for headers
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

    public byte[] generate(Payment payment) {
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

            String paidAt = payment.getPaidAt() != null
                    ? payment.getPaidAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    : "N/A";

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(0);
            rightCell.setBackgroundColor(WHITE);
            rightCell.setPadding(0);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(para("PAYMENT RECEIPT", bold(14, BLACK), Element.ALIGN_RIGHT));
            rightCell.addElement(para("Receipt #: " + payment.getReceiptNumber(), regular(8, MUTED), Element.ALIGN_RIGHT));
            rightCell.addElement(para("Payment Date: " + paidAt, bold(9, BLACK), Element.ALIGN_RIGHT));
            header.addCell(rightCell);
            doc.add(header);

            doc.add(spacer(3));
            addHRule(doc, BLACK);

            // 2. Info
            doc.add(spacer(2));
            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);

            String tenantName = safeGet(payment, "tenantName");
            String tenantPhone = safeGet(payment, "tenantPhone");
            String creditBal = safeGet(payment, "creditBalance");
            String receivedBy = safeGet(payment, "receivedBy");

            info.addCell(infoBlock("RECEIVED FROM",
                    new String[] { tenantName, "Phone: " + tenantPhone, "Account Credit Balance: $" + creditBal }));
            info.addCell(infoBlock("PAYMENT AUTHORIZATION",
                    new String[] { "Received By: " + receivedBy, "Status: VERIFIED & PROCESSED" }));
            doc.add(info);
            doc.add(spacer(1));

            // 4. Allocations Table
            addSectionLabel(doc, "Payment Allocations Breakdown");

            PdfPTable allocTable = new PdfPTable(5);
            allocTable.setWidthPercentage(100);
            allocTable.setWidths(new float[] { 1f, 1f, 1f, 1.2f, 1.2f });
            allocTable.setSpacingAfter(8);

            allocTable.addCell(headerCell("INVOICE PERIOD"));
            allocTable.addCell(headerCell("ROOM NUMBER"));
            allocTable.addCell(headerCell("AMOUNT APPLIED"));
            allocTable.addCell(headerCell("INV. REMAINING BALANCE"));
            allocTable.addCell(headerCell("INVOICE STATUS"));

            if (payment.getAllocations() == null || payment.getAllocations().isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase(
                        "No specific invoices allocated. Entire amount credited to tenant account balance.",
                        italic(9, MUTED)));
                emptyCell.setColspan(5);
                emptyCell.setBorderColor(BORDER);
                emptyCell.setPadding(10);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                allocTable.addCell(emptyCell);
            } else {
                for (PaymentAllocation alloc : payment.getAllocations()) {
                    Invoice inv = alloc.getInvoice();
                    String period = inv != null ? inv.getPeriod() : "N/A";
                    String roomN = (inv != null && inv.getContract() != null && inv.getContract().getRoom() != null)
                            ? inv.getContract().getRoom().getRoomNumber()
                            : "N/A";
                    String applied = "$" + alloc.getAmountApplied();
                    String balance = inv != null ? "$" + inv.getBalanceRemaining() : "N/A";
                    String status = inv != null ? inv.getStatus().name() : "N/A";

                    allocTable.addCell(dataCell(period));
                    allocTable.addCell(dataCell(roomN));
                    allocTable.addCell(dataCell(applied));
                    allocTable.addCell(dataCell(balance));
                    allocTable.addCell(statusCell(status));
                }
            }
            doc.add(allocTable);

            // 5. Right-aligned total summary table
            PdfPTable totalRow = new PdfPTable(2);
            totalRow.setWidthPercentage(50);
            totalRow.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalRow.setWidths(new float[] { 2.0f, 1f });
            totalRow.setSpacingAfter(4);

            PdfPCell totalLabel = new PdfPCell();
            totalLabel.setBorderColor(BORDER);
            totalLabel.setPadding(5);
            totalLabel.addElement(new Paragraph("Total Payment Amount Received", bold(10, BLACK)));
            totalRow.addCell(totalLabel);

            PdfPCell totalAmount = new PdfPCell(new Phrase("$" + payment.getAmount(), bold(10, BLACK)));
            totalAmount.setBorderColor(BORDER);
            totalAmount.setPadding(5);
            totalAmount.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalAmount.setVerticalAlignment(Element.ALIGN_MIDDLE);
            totalRow.addCell(totalAmount);
            doc.add(totalRow);

            Paragraph footnote = new Paragraph("* Paid Outstanding Invoices First", italic(8, MUTED));
            footnote.setAlignment(Element.ALIGN_RIGHT);
            doc.add(footnote);
            doc.add(spacer(16));

            // 6. Signature / Footer
            addHRule(doc, BORDER);
            doc.add(spacer(6));

            PdfPTable sigRow = new PdfPTable(2);
            sigRow.setWidthPercentage(100);

            PdfPCell sigLeft = new PdfPCell();
            sigLeft.setBorder(0);
            sigLeft.addElement(new Paragraph("Received By Signature:", regular(9, MUTED)));
            sigLeft.addElement(spacer(22));
            sigLeft.addElement(new Paragraph("_________________________________", regular(9, MUTED)));
            sigLeft.addElement(new Paragraph(receivedBy, regular(9, BLACK)));
            sigRow.addCell(sigLeft);

            PdfPCell sigRight = new PdfPCell();
            sigRight.setBorder(0);
            sigRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
            sigRight.addElement(para(
                    "This is an official computer-generated receipt from\nApartment Hub system. Please retain this receipt for your financial records.",
                    italic(9, MUTED), Element.ALIGN_RIGHT));
            sigRow.addCell(sigRight);
            doc.add(sigRow);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate payment receipt PDF", e);
        }
    }

    private String safeGet(Payment p, String field) {
        try {
            return switch (field) {
                case "tenantName" -> p.getTenant().getFullName();
                case "tenantPhone" -> {
                    String ph = p.getTenant().getPhone();
                    yield ph != null ? ph : "N/A";
                }
                case "creditBalance" -> p.getTenant().getCreditBalance().toPlainString();
                case "receivedBy" -> p.getReceivedBy() != null ? p.getReceivedBy().getFullName() : "Authorized Staff";
                default -> "N/A";
            };
        } catch (Exception e) {
            return "N/A";
        }
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
        p.setSpacingAfter(2);
        doc.add(p);
    }

    private PdfPCell headerCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, bold(8, BLACK)));
        c.setBackgroundColor(LIGHT_BG);
        c.setBorderColor(BORDER);
        c.setPadding(7);
        return c;
    }

    private PdfPCell dataCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, regular(9, BLACK)));
        c.setBackgroundColor(WHITE);
        c.setBorderColor(BORDER);
        c.setPadding(7);
        return c;
    }

    private PdfPCell statusCell(String status) {
        PdfPCell c = new PdfPCell(new Phrase(status, bold(8, BLACK)));
        c.setBackgroundColor(WHITE);
        c.setBorderColor(BORDER);
        c.setPadding(7);
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
}
