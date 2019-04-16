/**
 * Copyright 2010-2019 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mybatis.spring.annotation;

import org.mybatis.spring.mapper.ClassPathMapperScanner;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A {@link ImportBeanDefinitionRegistrar} to allow annotation configuration of
 * MyBatis mapper scanning. Using an @Enable annotation allows beans to be
 * registered via @Component configuration, whereas implementing
 * {@code BeanDefinitionRegistryPostProcessor} will work for XML configuration.
 *
 * @author Michael Lanyon
 * @author Eduardo Macarron
 * @author Putthiphong Boonphong
 * <p>
 * 实现 ImportBeanDefinitionRegistrar、ResourceLoaderAware 接口，@MapperScann 的注册器，负责将扫描到的 Mapper 接口，
 * 注册成 beanClass 为 MapperFactoryBean 的 BeanDefinition 对象，从而实现创建 Mapper 对象
 * @see MapperFactoryBean
 * @see ClassPathMapperScanner
 * @since 1.2.0
 */
public class MapperScannerRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    /**
     * ResourceLoader 对象
     */
    private ResourceLoader resourceLoader;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        // <1> 获得 @MapperScan 注解信息
        AnnotationAttributes mapperScanAttrs = AnnotationAttributes
                .fromMap(importingClassMetadata.getAnnotationAttributes(MapperScan.class.getName()));

        if (mapperScanAttrs != null) {
            // <2> 扫描包，将扫描到的 Mapper 接口，注册成 beanClass 为 MapperFactoryBean 的 BeanDefinition 对象
            registerBeanDefinitions(mapperScanAttrs, registry);
        }
    }

    void registerBeanDefinitions(AnnotationAttributes annoAttrs, BeanDefinitionRegistry registry) {
        // <3.1> 创建 ClassPathMapperScanner 对象
        ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);

        // <3.2> 设置 resourceLoader 属性到 scanner 中
        // this check is needed in Spring 3.1
        Optional.ofNullable(resourceLoader).ifPresent(scanner::setResourceLoader);

        Class<? extends Annotation> annotationClass = annoAttrs.getClass("annotationClass");
        if (!Annotation.class.equals(annotationClass)) {
            scanner.setAnnotationClass(annotationClass);
        }

        // <3.3> 获得 @(MapperScan 注解上的属性，设置到 scanner 中
        Class<?> markerInterface = annoAttrs.getClass("markerInterface");
        if (!Class.class.equals(markerInterface)) {
            scanner.setMarkerInterface(markerInterface);
        }

        Class<? extends BeanNameGenerator> generatorClass = annoAttrs.getClass("nameGenerator");
        if (!BeanNameGenerator.class.equals(generatorClass)) {
            scanner.setBeanNameGenerator(BeanUtils.instantiateClass(generatorClass));
        }

        Class<? extends MapperFactoryBean> mapperFactoryBeanClass = annoAttrs.getClass("factoryBean");
        if (!MapperFactoryBean.class.equals(mapperFactoryBeanClass)) {
            scanner.setMapperFactoryBean(BeanUtils.instantiateClass(mapperFactoryBeanClass));
        }

        scanner.setSqlSessionTemplateBeanName(annoAttrs.getString("sqlSessionTemplateRef"));
        scanner.setSqlSessionFactoryBeanName(annoAttrs.getString("sqlSessionFactoryRef"));

        // <4> 获得要扫描的包
        List<String> basePackages = new ArrayList<>();
        basePackages.addAll(
                Arrays.stream(annoAttrs.getStringArray("value"))    // 包
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toList()));

        basePackages.addAll(
                Arrays.stream(annoAttrs.getStringArray("basePackages")) // 包
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toList()));

        basePackages.addAll(
                Arrays.stream(annoAttrs.getClassArray("basePackageClasses"))    // 类
                        .map(ClassUtils::getPackageName)
                        .collect(Collectors.toList()));

        // <5> 注册 scanner 的过滤器
        scanner.registerFilters();

        // <6> 执行扫描
        scanner.doScan(StringUtils.toStringArray(basePackages));
    }

    /**
     * A {@link MapperScannerRegistrar} for {@link MapperScans}.
     *
     *  MapperScannerRegistrar 的内部静态类，继承 MapperScannerRegistrar 类，@MapperScans 的注册器
     *
     * @since 2.0.0
     */
    static class RepeatingRegistrar extends MapperScannerRegistrar {
        /**
         * {@inheritDoc}
         */
        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                            BeanDefinitionRegistry registry) {
            // 获得 @MapperScans 注解信息
            AnnotationAttributes mapperScansAttrs = AnnotationAttributes
                    .fromMap(importingClassMetadata.getAnnotationAttributes(MapperScans.class.getName()));

            // 遍历 @MapperScans 的值，调用 `#registerBeanDefinitions(mapperScanAttrs, registry)` 方法，循环扫描处理
            if (mapperScansAttrs != null) {
                Arrays.stream(mapperScansAttrs.getAnnotationArray("value"))
                        .forEach(mapperScanAttrs -> registerBeanDefinitions(mapperScanAttrs, registry));
            }
        }
    }

}
