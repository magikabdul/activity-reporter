package cloud.cholewa.reporter.trecom.mapper;

import cloud.cholewa.reporter.trecom.model.CreatedTaskResponse;
import cloud.cholewa.reporter.trecom.model.ReportResponse;
import cloud.cholewa.reporter.trecom.model.Task;
import cloud.cholewa.reporter.trecom.model.TaskEntity;
import cloud.cholewa.reporter.trecom.model.TaskResponse;
import cloud.cholewa.reporter.trecom.model.UpdateTaskRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TrecomMapper {

    @Mapping(target = "salesman.lastName", source = "salesmanLastName")
    @Mapping(target = "salesman.firstName", source = "salesmanFirstName")
    CreatedTaskResponse toResponse(Task task);

    @Mapping(target = "customerName", source = "customer")
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

    @Mapping(target = "salesman.lastName", source = "salesmanLastName")
    @Mapping(target = "salesman.firstName", source = "salesmanFirstName")
    @Mapping(target = "customer", source = "customerName")
    TaskResponse toTaskResponse(TaskEntity taskEntity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerName", expression = "java(org.apache.commons.lang3.StringUtils.upperCase(request.getCustomer()))")
    @Mapping(target = "salesmanFirstName", source = "salesman.firstName")
    @Mapping(target = "salesmanLastName", source = "salesman.lastName")
    void updateEntity(UpdateTaskRequest request, @MappingTarget TaskEntity taskEntity);
}
