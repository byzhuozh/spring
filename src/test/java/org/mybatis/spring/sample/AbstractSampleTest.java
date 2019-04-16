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
package org.mybatis.spring.sample;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.sample.domain.User;
import org.mybatis.spring.sample.service.FooService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * 示例单元测试基类,它有五种实现类
 *
 * 1. {@link SampleEnableTest}
 *      基于 @MapperScan 注解，扫描指定包
 *
 * 2. {@link SampleMapperTest}
 *      基于 {@link org.mybatis.spring.mapper.MapperFactoryBean} 类，直接声明指定的 Mapper 接口
 *
 * 3. {@link SampleNamespaceTest}
 *      基于 <mybatis:scan /> 标签，扫描指定包
 *
 * 4. {@link SampleScannerTest}
 *      基于 {@link org.mybatis.spring.mapper.MapperScannerConfigurer} 类，扫描指定包
 *
 * 5. {@link SampleBatchTest}
 *      在 SampleMapperTest 的基础上，使用 BatchExecutor 执行器
 */
@DirtiesContext
abstract class AbstractSampleTest {

    @Autowired
    protected FooService fooService;

    @Test
    final void testFooService() {
        User user = this.fooService.doSomeBusinessStuff("u1");
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("Pocoyo");
    }

}
