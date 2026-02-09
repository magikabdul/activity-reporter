package cloud.cholewa.reporter.trecom.mapper;

import cloud.cholewa.reporter.trecom.model.CreatedTaskResponse;
import cloud.cholewa.reporter.trecom.model.ReportResponse;
import cloud.cholewa.reporter.trecom.model.Task;
import cloud.cholewa.reporter.trecom.model.TaskEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TrecomMapper {

    @Mapping(target = "salesman.lastName", source = "salesmanLastName")
    @Mapping(target = "salesman.firstName", source = "salesmanFirstName")
    CreatedTaskResponse toResponse(Task task);

    @Mapping(target = "customerName", source = "customer")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "id", ignore = true)
    TaskEntity toEntity(Task task);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "salesman.lastName", source = "salesmanLastName")
    @Mapping(target = "salesman.firstName", source = "salesmanFirstName")
    @Mapping(target = "customer", source = "customerName")
    CreatedTaskResponse toResponse(TaskEntity taskEntity);

    @Mapping(target = "salesman", expression = "java(taskEntity.getSalesmanFirstName() + ' ' + taskEntity.getSalesmanLastName())")
    @Mapping(target = "company", source = "customerName")
    ReportResponse toReportResponse(TaskEntity taskEntity);
}
