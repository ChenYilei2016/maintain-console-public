package io.github.chenyilei2016.maintain.client.common.dto;

public class RuntimeMetadataParamSignDTO extends BaseSignDTO {
    private static final long serialVersionUID = 1L;

    @Override
    public String signData() {
        return "runtime-metadata";
    }
}
