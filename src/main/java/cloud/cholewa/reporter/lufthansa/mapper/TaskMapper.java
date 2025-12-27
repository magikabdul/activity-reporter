package cloud.cholewa.reporter.lufthansa.mapper;

import cloud.cholewa.reporter.lufthansa.model.CreatedTaskResponse;
import cloud.cholewa.reporter.lufthansa.model.Task;
import cloud.cholewa.reporter.lufthansa.model.TaskEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskMapper {


    CreatedTaskResponse toResponse(Task task);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDate.now())")
    TaskEntity toEntity(Task task);

    @Mapping(target = "id", ignore = true)
    CreatedTaskResponse toResponse(TaskEntity entity);
}
