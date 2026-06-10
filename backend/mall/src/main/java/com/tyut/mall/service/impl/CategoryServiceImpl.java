package com.tyut.mall.service.impl;

import com.tyut.mall.dto.response.CategoryVO;
import com.tyut.mall.repository.CategoryRepository;
import com.tyut.mall.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryVO> list() {
        return categoryRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(c -> CategoryVO.builder()
                        .id(String.valueOf(c.getId()))
                        .name(c.getName())
                        .build())
                .collect(Collectors.toList());
    }
}
