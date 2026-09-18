package cloud.cholewa.reporter.lufthansa.mapper;

import cloud.cholewa.reporter.lufthansa.model.CreatedTaskResponse;
import cloud.cholewa.reporter.lufthansa.model.Task;
import cloud.cholewa.reporter.lufthansa.model.TaskEntity;
import cloud.cholewa.reporter.lufthansa.model.TaskResponse;
import cloud.cholewa.reporter.lufthansa.model.UpdateTaskRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    CreatedTaskResponse toResponse(Task task);

    @Mapping(target = "id", ignore = true)
    TaskEntity toEntity(Task task);

    @Mapping(target = "id", ignore = true)
    CreatedTaskResponse toResponse(TaskEntity entity);

    TaskResponse toTaskResponse(TaskEntity entity);

    @Mapping(target = "id", ignore = true)
    void updateEntity(UpdateTaskRequest request, @MappingTarget TaskEntity entity);
}
