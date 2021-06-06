package ch.so.agi.stats;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Index;

import javax.validation.constraints.NotNull;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(indexes = {
        @Index(columnList = "requestTime", name = "request_time_idx"),
        @Index(columnList = "wmsRequestType", name = "wms_request_type_idx"),
        @Index(columnList = "wmsSrs", name = "wms_srs_idx"),
        @Index(columnList = "wmsWidth", name = "wms_width_idx"),
        @Index(columnList = "wmsHeight", name = "wms_height_idx"),
        @Index(columnList = "dpi", name = "dpi_idx")
})
public class WmsRequest extends PanacheEntity {
    @NotNull
    @Column(unique=true)
    String md5;
    
    String ip;
    
    Date requestTime;
    
    String requestMethod;
    
    @Column(columnDefinition="TEXT")
    String request;
    
    String wmsRequestType;
    
    @Column(columnDefinition="TEXT")
    String wmsLayers;
    
    Integer wmsSrs;
    
    String wmsBbox;
    
    Integer wmsWidth;
    
    Integer wmsHeight;
    
    Double dpi;
}
