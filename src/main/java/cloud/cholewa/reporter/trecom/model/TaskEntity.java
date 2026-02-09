package cloud.cholewa.reporter.trecom.model;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Data
@Builder
@Table("trecom")
public class TaskEntity {
    
    @Id
    private Long id;
    
    @NotNull
    private LocalDate createdAt;
    
    @NotNull
    private String customerName;
    
    @NotNull
    private String description;
    
    @NotNull
    private int hoursSpent;
    
    @NotNull
    @Column("salesperson_first_name")
    private String salesmanFirstName;
    
    @NotNull
    @Column("salesperson_last_name")
    private String salesmanLastName;
    
    @Nullable
    private String notes;
}
