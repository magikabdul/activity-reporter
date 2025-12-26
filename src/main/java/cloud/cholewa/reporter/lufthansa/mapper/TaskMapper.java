package cloud.cholewa.reporter.lufthansa.mapper;

import cloud.cholewa.reporter.lufthansa.model.CreateTaskRequest;
import cloud.cholewa.reporter.lufthansa.model.CreatedTaskResponse;
import cloud.cholewa.reporter.lufthansa.model.TaskCategory;
import cloud.cholewa.reporter.lufthansa.model.TaskEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "description", source = "request.description")
    TaskEntity toEntity(CreateTaskRequest request, TaskCategory category);

    CreatedTaskResponse toResponse(TaskEntity entity);
}
