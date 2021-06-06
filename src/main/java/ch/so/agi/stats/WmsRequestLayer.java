package ch.so.agi.stats;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(indexes = {
        @Index(columnList = "request_id", name = "request_id_idx"),
        @Index(columnList = "layerName", name = "layer_name_idx")
})
public class WmsRequestLayer extends PanacheEntity {
    @Column(length = 1024)
    String layerName;
    
    @ManyToOne
    @JoinColumn(name = "request_id")
    public WmsRequest request;
}
