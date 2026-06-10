package com.tyut.mall.service.impl;

import com.alibaba.fastjson2.JSON;
import com.tyut.mall.dto.response.ProductPageVO;
import com.tyut.mall.dto.response.ProductVO;
import com.tyut.mall.entity.BrowseHistory;
import com.tyut.mall.entity.Product;
import com.tyut.mall.repository.BrowseHistoryRepository;
import com.tyut.mall.repository.ProductRepository;
import com.tyut.mall.service.ProductService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final BrowseHistoryRepository browseHistoryRepository;

    @Override
    public ProductPageVO list(String keyword, String categoryCode, String brand, String stock,
                               String city, Double minPrice, Double maxPrice,
                               String sort, Integer page, Integer pageSize) {
        int pageNum = page != null ? page : 1;
        int size = pageSize != null ? pageSize : 6;
        if (size > 50) size = 50;
        String sortBy = sort != null ? sort : "composite";

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 只查上架商品
            predicates.add(cb.equal(root.get("status"), 1));

            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.trim().toLowerCase() + "%";
                Predicate p1 = cb.like(cb.lower(root.get("title")), kw);
                Predicate p2 = cb.like(cb.lower(root.get("subtitle")), kw);
                Predicate p3 = cb.like(cb.lower(root.get("brand")), kw);
                Predicate p4 = cb.like(cb.lower(root.get("tags")), kw);
                predicates.add(cb.or(p1, p2, p3, p4));
            }
            if (categoryCode != null && !categoryCode.isBlank()) {
                // categoryCode 和 Category 的 code 字段匹配，但 Product 存的是 categoryId
                // 这里做 join 查询更合适，简化处理: 用 categoryId 匹配
                try {
                    predicates.add(cb.equal(root.get("categoryId"), Long.valueOf(categoryCode)));
                } catch (NumberFormatException ignored) {}
            }
            if (brand != null && !brand.isBlank()) {
                predicates.add(cb.equal(root.get("brand"), brand));
            }
            if (stock != null) {
                if ("in".equals(stock)) {
                    predicates.add(cb.greaterThan(root.get("stock"), 0));
                } else if ("out".equals(stock)) {
                    predicates.add(cb.equal(root.get("stock"), 0));
                }
            }
            if (city != null && !city.isBlank()) {
                predicates.add(cb.equal(root.get("city"), city));
            }
            if (minPrice != null) {
                predicates.add(cb.ge(root.get("price"), BigDecimal.valueOf(minPrice)));
            }
            if (maxPrice != null) {
                predicates.add(cb.le(root.get("price"), BigDecimal.valueOf(maxPrice)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 排序
        Sort jpaSort = switch (sortBy) {
            case "priceAsc" -> Sort.by("price").ascending();
            case "priceDesc" -> Sort.by("price").descending();
            case "sales" -> Sort.by("sales").descending();
            case "popularity" -> Sort.by("popularity").descending();
            case "latest" -> Sort.by("createdAt").descending();
            default -> Sort.unsorted();  // composite: 内存排序或按 popularity+sales
        };

        Page<Product> productPage;
        if (Sort.unsorted().equals(jpaSort)) {
            productPage = productRepository.findAll(spec, PageRequest.of(pageNum - 1, size, Sort.by("popularity").descending()));
        } else {
            productPage = productRepository.findAll(spec, PageRequest.of(pageNum - 1, size, jpaSort));
        }

        List<ProductVO> products = productPage.getContent().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        // composite 排序: popularity + sales/100
        if (Sort.unsorted().equals(jpaSort)) {
            products.sort((a, b) -> {
                double scoreA = (a.getPopularity() != null ? a.getPopularity() : 0)
                        + (a.getSales() != null ? a.getSales() : 0) / 100.0;
                double scoreB = (b.getPopularity() != null ? b.getPopularity() : 0)
                        + (b.getSales() != null ? b.getSales() : 0) / 100.0;
                return Double.compare(scoreB, scoreA);
            });
        }

        return ProductPageVO.builder()
                .products(products)
                .total((int) productPage.getTotalElements())
                .page(pageNum)
                .pageSize(size)
                .totalPages(productPage.getTotalPages())
                .build();
    }

    @Override
    public ProductVO detail(Long id, Long userId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        if (product.getStatus() == 0) {
            throw new RuntimeException("商品已下架");
        }

        // 记录浏览历史
        if (userId != null) {
            browseHistoryRepository.deleteByUserIdAndProductId(userId, id);
            BrowseHistory history = BrowseHistory.builder()
                    .userId(userId)
                    .productId(id)
                    .build();
            browseHistoryRepository.save(history);
        }

        return toVO(product);
    }

    @Override
    public List<ProductVO> hot() {
        return productRepository.findByStatusOrderBySalesDesc(1)
                .stream().limit(8).map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<ProductVO> latest() {
        return productRepository.findByStatusOrderByCreatedAtDesc(1)
                .stream().limit(8).map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<ProductVO> special() {
        return productRepository.findAll().stream()
                .filter(p -> p.getStatus() == 1 && p.getOriginalPrice() != null)
                .limit(8).map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<ProductVO> recommended(Long userId) {
        if (userId == null) {
            return hot();
        }
        List<BrowseHistory> histories = browseHistoryRepository.findTop1ByUserIdOrderByCreatedAtDesc(userId);
        if (histories.isEmpty()) {
            return hot();
        }
        Long lastProductId = histories.get(0).getProductId();
        Product lastProduct = productRepository.findById(lastProductId).orElse(null);
        if (lastProduct == null) {
            return hot();
        }
        List<Product> related = productRepository
                .findByCategoryIdAndStatusAndIdNotOrderBySalesDesc(lastProduct.getCategoryId(), 1, lastProductId);
        List<ProductVO> result = related.stream().limit(4).map(this::toVO).collect(Collectors.toList());
        result.addAll(hot());
        return result.stream().distinct().limit(8).collect(Collectors.toList());
    }

    private ProductVO toVO(Product p) {
        return ProductVO.builder()
                .id(String.valueOf(p.getId()))
                .title(p.getTitle())
                .subtitle(p.getSubtitle())
                .category(p.getCategoryId() != null ? String.valueOf(p.getCategoryId()) : null)
                .brand(p.getBrand())
                .origin(p.getOrigin())
                .city(p.getCity())
                .tags(p.getTags() != null ? JSON.parseArray(p.getTags(), String.class) : List.of())
                .price(p.getPrice())
                .originalPrice(p.getOriginalPrice())
                .stock(p.getStock())
                .sales(p.getSales())
                .popularity(p.getPopularity())
                .rating(p.getRating())
                .image(p.getImage())
                .specs(p.getSpecs() != null ? JSON.parseArray(p.getSpecs(), String.class) : List.of())
                .promotion(p.getPromotion())
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null)
                .build();
    }
}
