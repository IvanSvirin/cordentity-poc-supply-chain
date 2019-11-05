/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.luxoft.poc.supplychain.data.schema

import com.luxoft.poc.supplychain.data.state.Package
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Lob
import javax.persistence.Table

object PackageSchema

object PackageSchemaV1 : MappedSchema(
        version = 1,
        schemaFamily = PackageSchema.javaClass,
        mappedTypes = listOf(PersistentPackage::class.java)) {

    @Entity
    @Table(name = "package")
    class PersistentPackage(

            @Column(name = "serial")
            var serial: String,

            @Column(name = "owner")
            @Lob
            var owner: ByteArray,

            @Column(name = "state")
            var state: Int,

            @Column
            var patientDid: String

    ) : PersistentState() {
        constructor(product: Package): this(
                product.info.serial,
                product.owner.owningKey.encoded,
                product.info.state.ordinal,
                product.info.patientDid)
        constructor(): this("", ByteArray(0), -1, "")
    }
}
