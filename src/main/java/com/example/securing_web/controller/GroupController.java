package com.example.securing_web.controller;

import com.example.securing_web.entity.StudentGroup;
import com.example.securing_web.entity.User;
import com.example.securing_web.service.GroupService;
import com.example.securing_web.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin/groups")
@PreAuthorize("hasRole('ADMIN')")
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;

    public GroupController(GroupService groupService, UserService userService) {
        this.groupService = groupService;
        this.userService = userService;
    }

    @GetMapping
    public String listGroups(Model model) {
        List<StudentGroup> groups = groupService.findAllGroups();
        List<User> teachers = userService.findUsersByRole("ROLE_TEACHER");
        List<User> studentsWithoutGroup = groupService.getStudentsWithoutGroup();

        model.addAttribute("groups", groups);
        model.addAttribute("teachers", teachers);
        model.addAttribute("studentsWithoutGroup", studentsWithoutGroup);
        model.addAttribute("totalGroups", groupService.getTotalGroupsCount());
        model.addAttribute("totalStudentsInGroups", groupService.getTotalStudentsInGroups());

        return "admin/groups";
    }

    @PostMapping("/create")
    public String createGroup(@RequestParam String name,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) Long teacherId,
                              RedirectAttributes redirectAttributes) {
        try {
            groupService.createGroup(name, description, teacherId);
            redirectAttributes.addFlashAttribute("success", "Группа создана успешно");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка создания группы: " + e.getMessage());
        }
        return "redirect:/admin/groups";
    }

    @PostMapping("/{id}/update")
    public String updateGroup(@PathVariable Long id,
                              @RequestParam(required = false) String name,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) Long teacherId,
                              RedirectAttributes redirectAttributes) {
        try {
            groupService.updateGroup(id, name, description, teacherId);
            redirectAttributes.addFlashAttribute("success", "Группа обновлена успешно");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка обновления группы: " + e.getMessage());
        }
        return "redirect:/admin/groups";
    }

    @PostMapping("/{id}/delete")
    public String deleteGroup(@PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        try {
            groupService.deleteGroup(id);
            redirectAttributes.addFlashAttribute("success", "Группа удалена успешно");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления группы: " + e.getMessage());
        }
        return "redirect:/admin/groups";
    }

    @PostMapping("/{groupId}/add-student")
    public String addStudentToGroup(@PathVariable Long groupId,
                                    @RequestParam Long studentId,
                                    RedirectAttributes redirectAttributes) {
        try {
            groupService.addStudentToGroup(groupId, studentId);
            redirectAttributes.addFlashAttribute("success", "Студент добавлен в группу");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка добавления студента: " + e.getMessage());
        }
        return "redirect:/admin/groups#group-" + groupId;
    }

    @PostMapping("/{groupId}/remove-student/{studentId}")
    public String removeStudentFromGroup(@PathVariable Long groupId,
                                         @PathVariable Long studentId,
                                         RedirectAttributes redirectAttributes) {
        try {
            groupService.removeStudentFromGroup(groupId, studentId);
            redirectAttributes.addFlashAttribute("success", "Студент удален из группы");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления студента: " + e.getMessage());
        }
        return "redirect:/admin/groups#group-" + groupId;
    }

    @GetMapping("/{id}/students")
    public String viewGroupStudents(@PathVariable Long id, Model model) {
        StudentGroup group = groupService.findGroupById(id)
                .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));
        List<User> students = groupService.getGroupStudents(id);

        model.addAttribute("group", group);
        model.addAttribute("students", students);

        return "admin/group-students";
    }
}