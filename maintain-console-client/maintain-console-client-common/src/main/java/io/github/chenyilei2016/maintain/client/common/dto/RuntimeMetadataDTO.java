package io.github.chenyilei2016.maintain.client.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class RuntimeMetadataDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int protocolVersion = 1;
    private List<ExposedBeanDTO> beans = new ArrayList<>();

    @Data
    public static class ExposedBeanDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private String type;
        private List<String> methods = new ArrayList<>();
    }
}
