/*
    Copyright 2017 Litrik De Roy

    This file is part of KCBJ Sponsors.

    KCBJ Sponsors is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    KCBJ Sponsors is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with KCBJ Sponsors.  If not, see <http://www.gnu.org/licenses/>.

 */

package be.kcbj.placemat;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPCellEvent;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import be.kcbj.core.SponsorManager;
import be.kcbj.core.model.Sponsor;

public class Placemat {

    private static final InsetCell CELL_EVENT = new InsetCell();
    private static final String DEST = "build/placemat.pdf";

    private static final int PADDING_DOC = 4;

    public static void main(String[] args) throws IOException, DocumentException {
        File file = new File(DEST);
        file.getParentFile().mkdirs();

        new Placemat().createPdf(file, SponsorManager.loadSponsors());
    }

    private void createPdf(File file, List<Sponsor> sponsors) throws IOException, DocumentException {
        System.out.println("Generating PDF file " + file.getAbsolutePath());
        Layout layout = new Layout(sponsors);
        System.out.println("Layout = " + layout);

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.setPageSize(PageSize.A4.rotate());
        document.setMargins(PADDING_DOC,  PADDING_DOC, PADDING_DOC, PADDING_DOC);
        document.open();

        PdfPTable table = new PdfPTable(layout.getColumnCount());
        table.setWidthPercentage(100);
        table.setSpacingBefore(0f);
        table.setSpacingAfter(0f);
        for (int i = 0; i < sponsors.size(); i++) {
            table.addCell(generateCell(sponsors.get(i), layout.getCellHeight()));
        }
        for (int i = 0; i < layout.getEmptyCellCount(); i++) {
            table.addCell(generateCell(new Sponsor(), layout.getCellHeight()));
        }
        document.add(table);

        document.close();
    }

    private PdfPCell generateCell(Sponsor sponsor, float cellHeight) throws IOException, BadElementException {
        int numLines = 0;
        Paragraph p = new Paragraph();

        if (sponsor.image != null) {
            Image image = Image.getInstance(SponsorManager.getImageUrl(sponsor.image));
            if (sponsor.imageWidth != 0) {
                image.scaleToFit(sponsor.imageWidth, 1000);
            } else if (sponsor.imageHeight != 0) {
                image.scaleToFit(1000, sponsor.imageHeight);
            }
            Chunk imageChunk = new Chunk(image, 0, 0, true);
            p.add(imageChunk);
        }

        if (sponsor.twoColumns) {
            StringBuilder sb = new StringBuilder();
            if (sponsor.name != null) {
                sb.append(sponsor.name);
            }
            if (sponsor.name2 != null) {
                if (sb.length() > 0) {
                    sb.append(" - ");
                }
                sb.append(sponsor.name2);
            }
            if (sponsor.address != null) {
                if (sb.length() > 0) {
                    sb.append(" - ");
                }
                sb.append(sponsor.address);
            }
            if (sponsor.address2 != null) {
                if (sb.length() > 0) {
                    sb.append(" - ");
                }
                sb.append(sponsor.address2);
            }
            p.add(Chunk.NEWLINE);
            p.add(new Chunk(sb.toString(), new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL)));
            numLines++;
        } else {
            if (sponsor.twoRows && sponsor.image != null) {
                p.add(Chunk.NEWLINE);
            }
            if (sponsor.name != null) {
                p.add(generateFittedChunk(sponsor.name, Font.BOLD));
                numLines++;
            }
            if (sponsor.name2 != null) {
                p.add(Chunk.NEWLINE);
                p.add(generateFittedChunk(sponsor.name2, Font.BOLD));
                numLines++;
            }
            if (sponsor.address != null) {
                p.add(new Chunk("\n\n", new Font(Font.FontFamily.HELVETICA, 2, Font.NORMAL)));
                p.add(new Chunk(sponsor.address, new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL)));
                numLines++;
            }
            if (sponsor.address2 != null) {
                p.add(Chunk.NEWLINE);
                p.add(new Chunk(sponsor.address2, new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL)));
                numLines++;
            }
        }
        p.setPaddingTop(0);
        p.setSpacingBefore(0);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setMultipliedLeading(numLines <= 3 ? 1.3f : 1.1f);

        PdfPCell cell = new PdfPCell();
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setFixedHeight(cellHeight);
        if (sponsor.twoColumns) {
            cell.setColspan(2);
        }
        if (sponsor.twoRows) {
            cell.setRowspan(2);
            if (sponsor.image == null) {
                p.setMultipliedLeading(p.getMultipliedLeading() * 1.5f);
            }
        }
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setCellEvent(CELL_EVENT);
        cell.setPaddingBottom(4);
        cell.addElement(p);
        if (sponsor.isTodo()) {
            cell.setBackgroundColor(BaseColor.ORANGE);
        }

        return cell;
    }

    private Chunk generateFittedChunk(String text, int style) {
        float size = 9 - (text.length() - 10f) / 5f;
        return new Chunk(text, new Font(Font.FontFamily.HELVETICA, size, style));
    }

    private static class InsetCell implements PdfPCellEvent {

        static final float INSET = 1.5f;

        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            float x1 = position.getLeft() + INSET;
            float x2 = position.getRight() - INSET;
            float y1 = position.getTop() - INSET;
            float y2 = position.getBottom() + INSET;
            PdfContentByte canvas = canvases[PdfPTable.LINECANVAS];
            canvas.roundRectangle(x1, y1, x2 - x1, y2 - y1, INSET);
            canvas.setLineWidth(0.5);
            canvas.stroke();
        }
    }

    private class Layout {
        private int filledCellCount = 0;
        private final int columnCount;
        private int rowCount;
        private int emptyCellCount;
        private int twoRowCount;
        private int twoColumnCount;
        private float cellHeight;

        Layout(List<Sponsor> sponsors) {
            countCells(sponsors);
            columnCount = 8;
            rowCount = (int) Math.ceil(filledCellCount / getColumnCount());
            emptyCellCount = (columnCount - filledCellCount % columnCount) % columnCount;
            cellHeight = (595f - 2 * PADDING_DOC) / rowCount;
        }

        private void countCells(List<Sponsor> sponsors) {
            for (int i = 0; i < sponsors.size(); i++) {
                Sponsor sponsor = sponsors.get(i);
                filledCellCount++;
                if (sponsor.twoColumns) {
                    twoColumnCount++;
                    filledCellCount++;
                } else if (sponsor.twoRows) {
                    twoRowCount++;
                    filledCellCount++;
                }
            }
        }

        int getColumnCount() {
            return columnCount;
        }

        int getEmptyCellCount() {
            return emptyCellCount;
        }

        float getCellHeight() {
            return cellHeight;
        }

        @Override
        public String toString() {
            return "Measure{" +
                    "filledCellCount=" + filledCellCount +
                    ", columnCount=" + columnCount +
                    ", rowCount=" + rowCount +
                    ", emptyCellCount=" + emptyCellCount +
                    ", twoRowCount=" + twoRowCount +
                    ", twoColumnCount=" + twoColumnCount +
                    ", cellHeight=" + cellHeight +
                    '}';
        }
    }
}
