package com.tyut.mall.controller;

import com.tyut.mall.common.ApiResponse;
import com.tyut.mall.entity.*;
import com.tyut.mall.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final MessageRepository messageRepository;

    // ===== 数据看板 =====

    @GetMapping("/dashboard")
    public ApiResponse<?> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 今日统计
        long totalUsers = userRepository.count();
        long totalOrders = orderRepository.count();
        long totalProducts = productRepository.count();

        List<Order> allOrders = orderRepository.findAll();
        BigDecimal totalSales = allOrders.stream()
                .filter(o -> "paid".equals(o.getStatus()) || "shipping".equals(o.getStatus()) || "completed".equals(o.getStatus()))
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 今日订单
        LocalDate today = LocalDate.now();
        long todayOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().equals(today))
                .count();
        BigDecimal todaySales = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().equals(today))
                .filter(o -> "paid".equals(o.getStatus()) || "shipping".equals(o.getStatus()) || "completed".equals(o.getStatus()))
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 最近7天趋势
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            List<Order> dayOrders = allOrders.stream()
                    .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().equals(d))
                    .toList();
            BigDecimal daySales = dayOrders.stream()
                    .filter(o -> !o.getStatus().equals("cancelled"))
                    .map(Order::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", d.toString());
            point.put("orders", dayOrders.size());
            point.put("sales", daySales);
            trend.add(point);
        }

        // 订单状态分布
        Map<String, Long> statusCount = allOrders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

        // 热卖商品 Top 5
        List<Product> hotProducts = productRepository.findByStatusOrderBySalesDesc(1)
                .stream().limit(5).toList();
        List<Map<String, Object>> hotList = hotProducts.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("title", p.getTitle());
            m.put("sales", p.getSales());
            m.put("revenue", p.getPrice().multiply(BigDecimal.valueOf(p.getSales())));
            return m;
        }).collect(Collectors.toList());

        data.put("totalUsers", totalUsers);
        data.put("totalOrders", totalOrders);
        data.put("totalProducts", totalProducts);
        data.put("totalSales", totalSales);
        data.put("todayOrders", todayOrders);
        data.put("todaySales", todaySales);
        data.put("trend", trend);
        data.put("statusCount", statusCount);
        data.put("hotProducts", hotList);

        return ApiResponse.ok(data);
    }

    // ===== 商品管理 =====

    @GetMapping("/products")
    public ApiResponse<?> products(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        var pg = productRepository.findAll(PageRequest.of(page - 1, size, Sort.by("createdAt").descending()));
        return ApiResponse.ok(Map.of("list", pg.getContent(), "total", pg.getTotalElements()));
    }

    @PutMapping("/products/{id}/status")
    public ApiResponse<?> toggleProduct(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Product p = productRepository.findById(id).orElse(null);
        if (p == null) return ApiResponse.fail(404, "商品不存在");
        p.setStatus(body.getOrDefault("status", p.getStatus()));
        productRepository.save(p);
        return ApiResponse.ok();
    }

    // ===== 订单管理 =====

    @GetMapping("/orders")
    public ApiResponse<?> orders(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        var pg = orderRepository.findAll(PageRequest.of(page - 1, size, Sort.by("createdAt").descending()));
        return ApiResponse.ok(Map.of("list", pg.getContent(), "total", pg.getTotalElements()));
    }

    // ===== 用户管理 =====

    @GetMapping("/users")
    public ApiResponse<?> users(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        var pg = userRepository.findAll(PageRequest.of(page - 1, size, Sort.by("createdAt").descending()));
        List<Map<String, Object>> list = pg.getContent().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("phone", u.getPhone());
            m.put("level", u.getLevel());
            m.put("balance", u.getBalance());
            m.put("points", u.getPoints());
            m.put("growth", u.getGrowth());
            m.put("status", u.getStatus());
            m.put("createdAt", u.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        return ApiResponse.ok(Map.of("list", list, "total", pg.getTotalElements()));
    }

    /** 卖家发货 */
    @PutMapping("/orders/{id}/ship")
    public ApiResponse<?> shipOrder(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) return ApiResponse.fail(404, "订单不存在");
        if (!"paid".equals(order.getStatus())) return ApiResponse.fail(400, "只有已付款订单可以发货");
        order.setStatus("shipping");
        order.setDeliveryNo("YT" + String.format("%08d", new Random().nextInt(90_000_000) + 10_000_000));
        orderRepository.save(order);
        // 发站内信通知买家
        try {
            Message msg = Message.builder().userId(order.getUserId()).title("订单已发货")
                    .body("订单 " + order.getOrderNo() + " 已发货，运单号 " + order.getDeliveryNo() + "，请注意查收。")
                    .type("order").read(0).build();
            messageRepository.save(msg);
        } catch (Exception ignored) {}
        return ApiResponse.ok();
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<?> toggleUser(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        User u = userRepository.findById(id).orElse(null);
        if (u == null) return ApiResponse.fail(404, "用户不存在");
        u.setStatus(body.getOrDefault("status", u.getStatus()));
        userRepository.save(u);
        return ApiResponse.ok();
    }

    // ===== CSV 导出 =====

    @GetMapping("/export/orders")
    public void exportOrders(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=orders.csv");
        PrintWriter w = response.getWriter();
        w.println("﻿订单号,用户ID,金额,状态,退款状态,时间");
        for (Order o : orderRepository.findAll()) {
            w.printf("%s,%d,%s,%s,%s,%s\n", o.getOrderNo(), o.getUserId(), o.getTotal(), o.getStatus(),
                    o.getRefundStatus() != null ? o.getRefundStatus() : "", o.getCreatedAt());
        }
        w.flush();
    }

    @GetMapping("/export/users")
    public void exportUsers(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=users.csv");
        PrintWriter w = response.getWriter();
        w.println("﻿用户名,昵称,手机号,等级,余额,积分,成长值,状态");
        for (User u : userRepository.findAll()) {
            w.printf("%s,%s,%s,%s,%s,%d,%d,%d\n", u.getUsername(), u.getNickname(), u.getPhone(),
                    u.getLevel(), u.getBalance(), u.getPoints(), u.getGrowth(), u.getStatus());
        }
        w.flush();
    }
}
