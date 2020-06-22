package io.gd.generator.handler;

import io.gd.generator.annotation.Default;
import io.gd.generator.util.StringUtils;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;

public class AllMappingMysqlHandler extends MysqlHandler {
    private static String SPACE = " ";
    /**
     * 是否全部映射到表，false时default\notnull将不起作用
     */
    private boolean mappingAll = true;

    public AllMappingMysqlHandler() {
        super();
    }

    public AllMappingMysqlHandler(boolean useGeneratedKeys) {
        super(useGeneratedKeys);
    }

    public void setMappingAll(boolean mappingAll) {
        this.mappingAll = mappingAll;
    }

    @Override
    protected String getMysqlType(Field field) {
        Column column = field.getDeclaredAnnotation(Column.class);
        String notnull = "", primarykey = "", defaultstr = "";

        if (mappingAll) {
            Default aDefault = field.getDeclaredAnnotation(Default.class);

            if (aDefault != null) {
                notnull = "NOT NULL";

                String defalutVal = "'" + aDefault.value() + "'";

                //关键字不加引号
                if (aDefault.type() == Default.DefaultType.DBKEY) {
                    defalutVal = aDefault.value();
                }

                defaultstr = "DEFAULT " + defalutVal;
            }

            if (field.getDeclaredAnnotation(NotNull.class) != null
                    || field.getDeclaredAnnotation(NotBlank.class) != null
                    || field.getDeclaredAnnotation(NotEmpty.class) != null) {
                notnull = "NOT NULL";
            }

            if (column != null) {
                String columnDefinition = column.columnDefinition();

                if (!column.nullable()) {
                    notnull = "NOT NULL";
                }

                if (StringUtils.isNotBlank(columnDefinition)) {
                    if (!columnDefinition.toUpperCase().contains(notnull)) {
                        columnDefinition += SPACE + notnull;
                    }
                    if (!columnDefinition.toUpperCase().contains(defaultstr)) {
                        columnDefinition += SPACE + defaultstr;
                    }

                    return columnDefinition;
                }
            }
        }

        Id id = field.getDeclaredAnnotation(Id.class);

        if (id != null) {
            primarykey = "PRIMARY KEY";

            GeneratedValue generatedValue = field.getDeclaredAnnotation(GeneratedValue.class);

            if (generatedValue != null) {
                if (generatedValue.strategy() == GenerationType.IDENTITY) {
                    primarykey = "AUTO_INCREMENT" + SPACE + primarykey;
                }
            } else if (useGeneratedKeys) {
                primarykey = "AUTO_INCREMENT" + SPACE + primarykey;
            }
        }

        Type genericType = field.getType();
        String typeName = genericType.getTypeName().toLowerCase();

        String columntype = "";

        if (field.getDeclaredAnnotation(Lob.class) != null) {
            columntype = "blob";

            if (field.getType().isAssignableFrom(String.class)) {
                columntype = "longtext";
            }
        } else if (typeName.contains("boolean")) {
            columntype = "bit(1)";
        } else if (typeName.contains("date")) {
            columntype = "datetime";

            Temporal dateType = field.getDeclaredAnnotation(Temporal.class);
            if (dateType != null) {
                TemporalType value = dateType.value();
                if (value != null) {
                    columntype = value.name().toLowerCase();
                }
            }
        } else if (typeName.contains("long")) {
            int length = 32;
            if (column != null) {
                if (column.length() != 255 && column.length() > 0 && column.length() < 255) {
                    length = column.length();
                }
            }
            columntype = "bigInt(" + length + ")";
        } else if (typeName.contains("int")) {
            columntype = "int(11)";
        } else if (typeName.contains("string")) {
            if (id == null) {
                if (column == null) {
                    columntype = "varchar(255)";
                } else {
                    int length = column.length();
                    columntype = "varchar(" + length + ")";
                }
            } else {
                columntype = "BIGINT(20)";
            }
        } else if (field.getType().isEnum()) {
            Enumerated enumd = field.getDeclaredAnnotation(Enumerated.class);
            int length = 255;
            if (column != null && column.length() > 0) {
                length = column.length();
            }
            if (enumd != null) {
                EnumType value = enumd.value();
                if (value.equals(EnumType.ORDINAL)) {
                    columntype = "int(2)";
                }
                columntype = "varchar(" + length + ")";
            }
            columntype = "int(2)";
        } else if (field.getType().isAssignableFrom(BigDecimal.class)) {
            if (column == null) {
                columntype = "decimal(19,2)";
            } else {
                int precision = column.precision() == 0 ? 19 : column.precision();
                int scale = column.scale() == 0 ? 19 : column.scale();
                columntype = "decimal(" + precision + "," + scale + ")";
            }
        } else if (field.getType().isAssignableFrom(Float.class)) {
            if (column == null) {
                columntype = "float(9,2)";
            } else {
                int precision = column.precision() == 0 ? 9 : column.precision();
                int scale = column.scale() == 0 ? 9 : column.scale();
                columntype = "float(" + precision + "," + scale + ")";
            }
        } else if (field.getType().isAssignableFrom(Double.class)) {
            if (column == null) {
                columntype = "double(19,2)";
            } else {
                int precision = column.precision() == 0 ? 19 : column.precision();
                int scale = column.scale() == 0 ? 19 : column.scale();
                columntype = "double(" + precision + "," + scale + ")";
            }
        }

        if (StringUtils.isNotBlank(columntype)) {
            if (mappingAll) {
                return columntype + SPACE + notnull + SPACE + defaultstr + SPACE + primarykey;
            } else {
                return columntype + SPACE + primarykey;
            }
        }

        throw new RuntimeException(typeName + " 无法解析。请检查getMysqlType解析方法");
    }

}
