package com.example.grocerystore.service;

import com.example.grocerystore.model.CustomerOrder;
import com.example.grocerystore.model.PaymentChannel;
import com.example.grocerystore.repository.OrderRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final OrderRepository orderRepository;

    public ReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public byte[] generateMonthlySalesReport(int year, int month) throws Exception {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.plusMonths(1).atDay(1);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atStartOfDay();

        List<CustomerOrder> orders = orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);

        // Prepare workbook
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Monthly Sales");

            int rownum = 0;
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(rownum++);
            String[] cols = new String[]{"Order ID", "Tracking Code", "Customer Name", "Phone", "Total Amount", "Payment Channel", "KPay Amount", "Wave Amount", "COD Amount", "Bank Transfer Amount", "Order Status", "Order Date"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            BigDecimal totalRevenue = BigDecimal.ZERO;
            BigDecimal totalKpay = BigDecimal.ZERO;
            BigDecimal totalWave = BigDecimal.ZERO;
            BigDecimal totalCod = BigDecimal.ZERO;
            BigDecimal totalBank = BigDecimal.ZERO;

            for (CustomerOrder o : orders) {
                Row r = sheet.createRow(rownum++);
                int cidx = 0;
                r.createCell(cidx++).setCellValue(o.getId());
                r.createCell(cidx++).setCellValue(o.getTrackingCode());
                r.createCell(cidx++).setCellValue(o.getCustomerName());
                r.createCell(cidx++).setCellValue(o.getPhoneNumber());
                r.createCell(cidx++).setCellValue(o.getTotalAmount().doubleValue());
                PaymentChannel ch = o.getPaymentChannel() == null ? PaymentChannel.UNKNOWN : o.getPaymentChannel();
                r.createCell(cidx++).setCellValue(ch.name());

                double kpayAmt = 0, waveAmt = 0, codAmt = 0, bankAmt = 0;
                if (ch == PaymentChannel.KPAY) {
                    kpayAmt = o.getTotalAmount().doubleValue();
                    totalKpay = totalKpay.add(o.getTotalAmount());
                } else if (ch == PaymentChannel.WAVE) {
                    waveAmt = o.getTotalAmount().doubleValue();
                    totalWave = totalWave.add(o.getTotalAmount());
                } else if (ch == PaymentChannel.COD) {
                    codAmt = o.getTotalAmount().doubleValue();
                    totalCod = totalCod.add(o.getTotalAmount());
                } else if (ch == PaymentChannel.BANK_TRANSFER) {
                    bankAmt = o.getTotalAmount().doubleValue();
                    totalBank = totalBank.add(o.getTotalAmount());
                }

                r.createCell(cidx++).setCellValue(kpayAmt);
                r.createCell(cidx++).setCellValue(waveAmt);
                r.createCell(cidx++).setCellValue(codAmt);
                r.createCell(cidx++).setCellValue(bankAmt);
                r.createCell(cidx++).setCellValue(o.getStatus().name());
                r.createCell(cidx++).setCellValue(o.getCreatedAt().toString());

                totalRevenue = totalRevenue.add(o.getTotalAmount());
            }

            // Summary row
            Row summary = sheet.createRow(rownum++);
            summary.createCell(0).setCellValue("TOTAL");
            summary.createCell(4).setCellValue(totalRevenue.doubleValue());
            summary.createCell(6).setCellValue(totalKpay.doubleValue());
            summary.createCell(7).setCellValue(totalWave.doubleValue());
            summary.createCell(8).setCellValue(totalCod.doubleValue());
            summary.createCell(9).setCellValue(totalBank.doubleValue());

            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    public Map<String, Object> salesOverview(LocalDateTime start, LocalDateTime end) {
        BigDecimal revenue = orderRepository.sumTotalAmountBetween(start, end);
        long orderCount = orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end).size();
        List<Object[]> topProducts = orderRepository.findTopSellingProducts(start, end);
        List<Map<String, Object>> tops = topProducts.stream().map(r -> Map.of(
                "productId", r[0],
                "productName", r[1],
                "quantity", r[2]
        )).collect(Collectors.toList());

        // payment channel distribution
        List<CustomerOrder> orders = orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        Map<String, Long> channelDist = orders.stream().collect(Collectors.groupingBy(o -> o.getPaymentChannel() == null ? "UNKNOWN" : o.getPaymentChannel().name(), Collectors.counting()));

        return Map.of("revenue", revenue == null ? BigDecimal.ZERO : revenue,
                "orderCount", orderCount,
                "topProducts", tops,
                "paymentDistribution", channelDist);
    }
}