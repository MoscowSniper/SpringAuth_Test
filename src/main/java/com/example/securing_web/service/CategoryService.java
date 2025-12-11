package com.example.securing_web.service;

import com.example.securing_web.entity.Category;
import com.example.securing_web.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));
    }

    public Category createCategory(String name, String description) {
        if (categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("Категория с таким именем уже существует");
        }

        Category category = new Category(name, description);
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Категория не найдена");
        }
        categoryRepository.deleteById(id);
    }
}