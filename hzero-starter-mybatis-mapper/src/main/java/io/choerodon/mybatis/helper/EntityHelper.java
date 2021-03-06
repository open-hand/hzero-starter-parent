/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2016 abel533@gmail.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.choerodon.mybatis.helper;

import io.choerodon.mybatis.MapperException;
import io.choerodon.mybatis.annotation.ColumnType;
import io.choerodon.mybatis.annotation.MultiLanguage;
import io.choerodon.mybatis.annotation.MultiLanguageField;
import io.choerodon.mybatis.code.DbType;
import io.choerodon.mybatis.code.IdentityDialect;
import io.choerodon.mybatis.code.Style;
import io.choerodon.mybatis.domain.Config;
import io.choerodon.mybatis.domain.EntityColumn;
import io.choerodon.mybatis.domain.EntityField;
import io.choerodon.mybatis.domain.EntityTable;
import io.choerodon.mybatis.helper.snowflake.annotation.SnowflakeExclude;
import io.choerodon.mybatis.util.SimpleTypeUtil;
import io.choerodon.mybatis.util.StringUtil;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.UnknownTypeHandler;
import org.hzero.mybatis.annotation.DataSecurity;
import org.hzero.mybatis.annotation.Unique;
import org.hzero.mybatis.common.query.JoinColumn;
import org.hzero.mybatis.common.query.JoinTable;
import org.hzero.mybatis.common.query.*;
import org.hzero.mybatis.domian.SecurityToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import javax.persistence.criteria.JoinType;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * 实体类工具类 - 处理实体和数据库表以及字段关键的一个类
 * <p>项目地址 : <a href="https://github.com/abel533/Mapper" target="_blank">https://github.com/abel533/Mapper</a></p>
 *
 * @author liuzh
 */
public class EntityHelper {
    private static final String MULTI_LANGUAGE_TABLE_SUFFIX_LOW_CASE = "_tl";
    private static final String MULTI_LANGUAGE_TABLE_SUFFIX_UPPER_CASE = "_TL";
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityHelper.class);
    /**
     * 实体类 => 表对象
     */
    private static final Map<Class<?>, EntityTable> entityClassTableMap = new ConcurrentHashMap<>();
    private static final Map<String, EntityTable> mapperClassTableMap = new ConcurrentHashMap<>();

    private static final Map<Class<?>, EntityTable> entityTableMap = new HashMap<>();

    private EntityHelper() {
    }

    /**
     * 是否包含实体类
     *
     * @param entityClass 实体类
     * @return 是否包含实体类
     */
    public static boolean contain(Class<?> entityClass) {
        return entityClassTableMap.containsKey(entityClass);
    }


    /**
     * 获取表对象
     *
     * @param entityClass entityClass
     * @return EntityTable
     */
    public static EntityTable getTableByEntity(Class<?> entityClass) {
        EntityTable entityTable = entityClassTableMap.get(entityClass);
        if (entityTable == null) {
            throw new MapperException("not found table for entity class: " + entityClass);
        }
        return entityTable;
    }

    /**
     * 获取表对象
     *
     * @param mapperClass mapperClass
     * @return EntityTable
     */
    public static EntityTable getTableByMapper(String mapperClass) {
        EntityTable entityTable = mapperClassTableMap.get(mapperClass);
        if (entityTable == null) {
            LOGGER.debug("not found table for mapper class: " + mapperClass);
        }
        return entityTable;
    }

    /**
     * 获取默认的orderby语句
     *
     * @param entityClass entityClass
     * @return String
     */
    public static String getOrderByClause(Class<?> entityClass) {
        EntityTable table = getTableByEntity(entityClass);
        if (table.getOrderByClause() != null) {
            return table.getOrderByClause();
        }
        StringBuilder orderBy = new StringBuilder();
        for (EntityColumn column : table.getEntityClassColumns()) {
            if (column.getOrderBy() != null) {
                if (orderBy.length() != 0) {
                    orderBy.append(",");
                }
                orderBy.append(column.getColumn()).append(" ").append(column.getOrderBy());
            }
        }
        table.setOrderByClause(orderBy.toString());
        return table.getOrderByClause();
    }

    /**
     * 获取全部列
     *
     * @param entityClass entityClass
     * @return Set
     */
    public static Set<EntityColumn> getColumns(Class<?> entityClass) {
        return getTableByEntity(entityClass).getEntityClassColumns();
    }

    /**
     * 获取主键信息
     *
     * @param entityClass entityClass
     * @return Set
     */
    public static Set<EntityColumn> getPkColumns(Class<?> entityClass) {
        return getTableByEntity(entityClass).getEntityClassPkColumns();
    }

    /**
     * 获取查询的Select
     *
     * @param entityClass entityClass
     * @return String
     */
    public static String getSelectColumns(Class<?> entityClass) {
        EntityTable entityTable = getTableByEntity(entityClass);
        if (entityTable.getBaseSelect() != null) {
            return entityTable.getBaseSelect();
        }
        Set<EntityColumn> columnList = getColumns(entityClass);
        StringBuilder selectBuilder = new StringBuilder();
        boolean skipAlias = Map.class.isAssignableFrom(entityClass);
        for (EntityColumn entityColumn : columnList) {
            selectBuilder.append(entityColumn.getColumn());
            if (!skipAlias && !entityColumn.getColumn().equalsIgnoreCase(entityColumn.getProperty())) {
                //不等的时候分几种情况，例如`DESC`
                if (entityColumn.getColumn().substring(1, entityColumn.getColumn().length() - 1)
                        .equalsIgnoreCase(entityColumn.getProperty())) {
                    selectBuilder.append(",");
                } else {
                    selectBuilder.append(" AS ").append(entityColumn.getProperty()).append(",");
                }
            } else {
                selectBuilder.append(",");
            }
        }
        entityTable.setBaseSelect(selectBuilder.substring(0, selectBuilder.length() - 1));
        return entityTable.getBaseSelect();
    }

    /**
     * 初始化实体属性
     *
     * @param entityClass entityClass
     * @param mapperClass mapperClass
     * @param config      config
     */
    public static synchronized void initEntityNameMap(Class<?> entityClass, String mapperClass, Config config) {
        if (entityClassTableMap.containsKey(entityClass)
                && mapperClassTableMap.containsKey(mapperClass)) {
            return;
        }
        Style style = config.getStyle();

        //创建并缓存EntityTable
        EntityTable entityTable = null;
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table table = entityClass.getAnnotation(Table.class);
            if (!"".equals(table.name())) {
                entityTable = new EntityTable(entityClass);
                entityTable.setTable(table);
            }
        }
        if (entityTable == null) {
            entityTable = new EntityTable(entityClass);
            //可以通过style控制
            entityTable.setName(StringUtil.convertByStyle(entityClass.getSimpleName(), style));
        }
        if (entityClass.isAnnotationPresent(MultiLanguage.class)) {
            entityTable.setMultiLanguage(true);
            String tableName = entityTable.getName();
            if (StringUtil.tableNameAllUpperCase(tableName)) {
                entityTable.setMultiLanguageTableName(tableName + MULTI_LANGUAGE_TABLE_SUFFIX_UPPER_CASE);
            } else {
                entityTable.setMultiLanguageTableName(tableName + MULTI_LANGUAGE_TABLE_SUFFIX_LOW_CASE);
            }
        }
        entityTable.setEntityClassColumns(new LinkedHashSet<>());
        entityTable.setEntityClassPkColumns(new LinkedHashSet<>());
        entityTable.setMultiLanguageColumns(new LinkedHashSet<>());
        entityTable.setDataSecurityColumns(new LinkedHashSet<>());
        entityTable.setEntityClassTransientColumns(new LinkedHashSet<>());
        entityTable.setUniqueColumns(new LinkedHashSet<>());
        //处理所有列
        List<EntityField> fields = null;
        if (config.isEnableMethodAnnotation()) {
            fields = FieldHelper.getAll(entityClass);
        } else {
            fields = FieldHelper.getFields(entityClass);
        }
        for (EntityField field : fields) {
            //如果启用了简单类型，就做简单类型校验，如果不是简单类型，直接跳过
            //3.5.0 如果启用了枚举作为简单类型，就不会自动忽略枚举类型
            //4.0 如果标记了 Column 或 ColumnType 注解，也不忽略
            if (config.isUseSimpleType()
                    && !field.isAnnotationPresent(Column.class)
                    && !field.isAnnotationPresent(ColumnType.class)
                    && !SimpleTypeUtil.isSimpleType(field.getJavaType())) {
                continue;
            }
            processField(entityTable, style, field, config);
        }
        //当pk.size=0的时候使用所有列作为主键
        if (entityTable.getEntityClassPkColumns().isEmpty()) {
            entityTable.setEntityClassPkColumns(entityTable.getEntityClassColumns());
        }
        entityTable.initPropertyMap();
        entityClassTableMap.put(entityClass, entityTable);
        mapperClassTableMap.put(mapperClass, entityTable);
        entityTableMap.put(entityClass, entityTable);
    }

    /**
     * 处理一列
     *
     * @param entityTable entityTable
     * @param style       style
     * @param field       field
     * @param config      config
     */
    private static void processField(EntityTable entityTable, Style style, EntityField field, Config config) {
        //Id
        EntityColumn entityColumn = new EntityColumn(entityTable);
        entityColumn.setField(field);
        if (field.isAnnotationPresent(Id.class)) {
            entityColumn.setId(true);
        }
        if (field.isAnnotationPresent(MultiLanguageField.class)) {
            entityColumn.setMultiLanguage(true);
        }
        if (field.isAnnotationPresent(SnowflakeExclude.class)) {
            entityColumn.setSnowflakeEnable(false);
        }

        //排除字段
        entityTable.appendField(field);
        entityColumn.setProperty(field.getName());
        entityColumn.setJavaType(field.getJavaType());
        //Column
        String columnName = null;
        if (field.isAnnotationPresent(Column.class)) {
            Column column = field.getAnnotation(Column.class);
            columnName = column.name();
            entityColumn.setUpdatable(column.updatable());
            entityColumn.setInsertable(column.insertable());
        }
        //ColumnType
        if (field.isAnnotationPresent(ColumnType.class)) {
            ColumnType columnType = field.getAnnotation(ColumnType.class);
            //是否为 blob 字段
            entityColumn.setBlob(columnType.isBlob());
            //column可以起到别名的作用
            if (StringUtil.isEmpty(columnName) && StringUtil.isNotEmpty(columnType.column())) {
                columnName = columnType.column();
            }
            if (columnType.jdbcType() != JdbcType.UNDEFINED) {
                entityColumn.setJdbcType(columnType.jdbcType());
            }
            if (columnType.typeHandler() != UnknownTypeHandler.class) {
                entityColumn.setTypeHandler(columnType.typeHandler());
            }
        }

        //列名
        if (StringUtil.isEmpty(columnName)) {
            columnName = StringUtil.convertByStyle(field.getName(), style);
        }
        entityColumn.setColumn(columnName);
        entityColumn.setField(field);
        if (entityTable.isMultiLanguage()) {
            if (field.isAnnotationPresent(Id.class)) {
                JoinTable jt = new JoinTable() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return JoinTable.class;
                    }

                    @Override
                    public String name() {
                        return "multiLanguageJoin";
                    }

                    @Override
                    public boolean joinMultiLanguageTable() {
                        return true;
                    }

                    @Override
                    public Class<?> target() {
                        return entityTable.getEntityClass();
                    }

                    @Override
                    public JoinType type() {
                        return JoinType.INNER;
                    }

                    @Override
                    public JoinOn[] on() {
                        JoinOn on1 = new JoinOn() {
                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return JoinOn.class;
                            }

                            @Override
                            public String joinField() {
                                return field.getName();
                            }

                            @Override
                            public String joinExpression() {
                                return "";
                            }
                        };

                        JoinOn on2 = new JoinOn() {
                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return JoinOn.class;
                            }

                            @Override
                            public String joinField() {
                                return "lang";
                            }

                            @Override
                            public String joinExpression() {
                                return "__current_locale";
                            }
                        };


                        return new JoinOn[]{on1, on2};
                    }


                };

                entityColumn.addJoinTable(jt);
                entityTable.createAlias(buildJoinKey(jt));
                entityTable.getJoinMapping().put(jt.name(), entityColumn);
            }

            if (field.isAnnotationPresent(MultiLanguageField.class)) {
                JoinColumn jc = new JoinColumn() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return JoinColumn.class;
                    }

                    @Override
                    public String joinName() {
                        return "multiLanguageJoin";
                    }

                    @Override
                    public String field() {
                        return field.getName();
                    }

                    @Override
                    public String expression() {
                        return "";
                    }
                };
                entityColumn.setJoinColumn(jc);
                entityColumn.setSelectable(true);
                entityColumn.setMultiLanguage(true);
            }

        }

        // @JoinTable
        JoinTable[] jts = field.getAnnotations(JoinTable.class);
        if (jts != null) {
            for (JoinTable joinTable : jts) {
                entityColumn.addJoinTable(joinTable);
                entityTable.createAlias(buildJoinKey(joinTable));
                entityTable.getJoinMapping().put(joinTable.name(), entityColumn);
            }
        }

        // @Where
        if (field.isAnnotationPresent(Where.class)) {
            Where where = field.getAnnotation(Where.class);
            entityColumn.setWhere(where);
            entityTable.getWhereColumns().add(entityColumn);
        }

        //OrderBy
        if (field.isAnnotationPresent(OrderBy.class)) {
            OrderBy orderBy = field.getAnnotation(OrderBy.class);
            if ("".equals(orderBy.value())) {
                entityColumn.setOrderBy("ASC");
            } else {
                entityColumn.setOrderBy(orderBy.value());
            }
            entityTable.getSortColumns().add(entityColumn);
        }

        // @JoinColumn
        if (field.isAnnotationPresent(JoinColumn.class)) {
            JoinColumn jc = field.getAnnotation(JoinColumn.class);
            entityColumn.setJoinColumn(jc);
            entityColumn.setSelectable(true);
            entityColumn.setInsertable(false);
            entityColumn.setUpdatable(false);
        }

        if (field.isAnnotationPresent(Id.class)) {
            entityColumn.setId(true);
            if (entityColumn.getWhere() == null) {
                Where where = new Where() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Where.class;
                    }

                    @Override
                    public Comparison comparison() {
                        return Comparison.EQUAL;
                    }

                    @Override
                    public String expression() {
                        return "";
                    }
                };
                entityColumn.setWhere(where);
                entityTable.getWhereColumns().add(entityColumn);
            }
        }
        // ColumnType

        //主键策略 - Oracle序列，MySql自动增长，UUID
        primaryKeyStrategy(entityTable, field, entityColumn, config);
        if (entityColumn.isMultiLanguage()) {
            entityTable.getMultiLanguageColumns().add(entityColumn);
        } else if (entityColumn.isId()) {
            entityTable.getEntityClassPkColumns().add(entityColumn);
        }
        // 数据加密
        if (field.isAnnotationPresent(DataSecurity.class)) {
            entityTable.setDataSecurity(true);
            entityTable.addDataSecurityColumns(entityColumn);
        }
        // 唯一列
        if (field.isAnnotationPresent(Unique.List.class)) {
            Unique[] annotations = field.getAnnotation(Unique.List.class).value();
            entityTable.addUniqueColumn(entityColumn
                    .setConstraintNames(Arrays.stream(annotations)
                            .map(EntityHelper::getConstraintName)
                            .collect(Collectors.toSet())));
            if (entityColumn.isMultiLanguage()) {
                entityTable.setMultiLanguageUnique(true);
            }
        } else if (field.isAnnotationPresent(Unique.class)) {
            Unique annotation = field.getAnnotation(Unique.class);
            entityTable.addUniqueColumn(entityColumn.setConstraintNames(Collections.singleton(getConstraintName(annotation))));
            if (entityColumn.isMultiLanguage()) {
                entityTable.setMultiLanguageUnique(true);
            }
        }
        if (field.isAnnotationPresent(Transient.class)) {
            if (SecurityToken.class.isAssignableFrom(field.getJavaType()) || Collection.class.isAssignableFrom(field.getJavaType())) {
                entityTable.addSecurityTokenField(field);
            }
            entityColumn.setInsertable(false);
            entityColumn.setUpdatable(false);
            entityTable.getEntityClassTransientColumns().add(entityColumn);
            return;
        }
        EntityColumn exists = entityTable.getEntityClassColumns().stream()
                .filter(item -> Objects.equals(item.getProperty(), entityColumn.getProperty()))
                .findFirst()
                .orElse(null);
        if (exists != null && Objects.equals(entityTable.getEntityClass(), entityColumn.getField().getField().getDeclaringClass())) {
            entityTable.getEntityClassColumns().remove(exists);
        }
        entityTable.getEntityClassColumns().add(entityColumn);
    }

    private static String getConstraintName(Unique unique) {
        if (!Unique.DEFAULT_CONSTRAINT_NAME.equals(unique.constraintName())) {
            return unique.constraintName();
        }
        return unique.value();
    }

    private static void primaryKeyStrategy(EntityTable entityTable, EntityField field, EntityColumn entityColumn, Config config) {
        if (field.isAnnotationPresent(SequenceGenerator.class)) {
            SequenceGenerator sequenceGenerator = field.getAnnotation(SequenceGenerator.class);
            if ("".equals(sequenceGenerator.sequenceName())) {
                throw new MapperException(entityTable.getEntityClass() + "字段" + field.getName() + "的注解@SequenceGenerator未指定sequenceName!");
            }
            entityColumn.setSequenceName(sequenceGenerator.sequenceName());
        } else if (field.isAnnotationPresent(GeneratedValue.class)) {
            GeneratedValue generatedValue = field.getAnnotation(GeneratedValue.class);
            if ("UUID".equals(generatedValue.generator())) {
                entityColumn.setUuid(true);
            } else if ("JDBC".equals(generatedValue.generator())) {
                DbType dbType = config.getDbType();
                if (DbType.SQLSERVER.equals(dbType)) {
                    entityColumn.setInsertable(false);
                }
                entityColumn.setIdentity(true);
                entityColumn.setGenerator("JDBC");
                entityTable.setKeyProperties(entityColumn.getProperty());
                entityTable.setKeyColumns(entityColumn.getColumn());
            } else {
                //允许通过generator来设置获取id的sql,例如mysql=CALL IDENTITY(),hsqldb=SELECT SCOPE_IDENTITY()
                //允许通过拦截器参数设置公共的generator
                DbType dbType = config.getDbType();
                if (DbType.SQLSERVER.equals(dbType)) {
                    entityColumn.setInsertable(false);
                }
                dealByGeneratedValueStrategy(entityTable, entityColumn, generatedValue);
            }
        }
    }

    private static void dealByGeneratedValueStrategy(EntityTable entityTable,
                                                     EntityColumn entityColumn, GeneratedValue generatedValue) {
        if (generatedValue.strategy() == GenerationType.IDENTITY) {
            //mysql的自动增长
            entityColumn.setIdentity(true);
            if (!"".equals(generatedValue.generator())) {
                String generator = null;
                IdentityDialect identityDialect =
                        IdentityDialect.getDatabaseDialect(generatedValue.generator());
                if (identityDialect != null) {
                    generator = identityDialect.getIdentityRetrievalStatement();
                } else {
                    generator = generatedValue.generator();
                }
                entityColumn.setGenerator(generator);
            }
        } else {
            entityColumn.setIdentity(true);
            //entityColumn.setGenerator("JDBC");
            entityTable.setKeyProperties(entityColumn.getProperty());
            entityTable.setKeyColumns(entityColumn.getColumn());
        }
    }

    /**
     * HZero新增内容
     */
    /**
     * 获取表对象
     *
     * @param entityClass
     * @return
     */
    public static EntityTable getEntityTable(Class<?> entityClass) {
        EntityTable entityTable = entityTableMap.get(entityClass);
        if (entityTable == null) {
            throw new MapperException("无法获取实体类" + entityClass.getCanonicalName() + "对应的表名!");
        }
        return entityTable;
    }

    /**
     * 获取主键信息
     *
     * @param entityClass
     * @return
     */
    public static Set<EntityColumn> getPKColumns(Class<?> entityClass) {
        return getEntityTable(entityClass).getEntityClassPkColumns();
    }

    public static String buildJoinKey(JoinTable jt) {
        return jt.target().getCanonicalName() + "." + jt.name();
    }

    public static String buildJoinTLKey(JoinTable jt) {
        return jt.target().getCanonicalName() + "_TL" + "." + jt.name();
    }
}