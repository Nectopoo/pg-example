package test.dev.smartreplication.example.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Size;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "rrko_edoc_ref", schema = "schema1")
public class EdocRef {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "edoc_ref_id_seq")
    @SequenceGenerator(name = "edoc_ref_id_seq", sequenceName = "edoc_ref_id_seq")
    private Long id;

    @Size(max = 255)
    @Column(name = "edoc_class_name")
    private String edocClassName;

    @Column(name = "edoc_id")
    private Long edocId;

    @Column(name = "client_snapshot_id")
    private Long clientSnapshotId;

    @Column(name = "branch_snapshot_id")
    private Long branchSnapshotId;

    public EdocRef(String edocClassName) {
        this.edocClassName = edocClassName;
    }
}
