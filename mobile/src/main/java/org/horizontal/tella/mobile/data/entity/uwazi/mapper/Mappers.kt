package org.horizontal.tella.mobile.data.entity.uwazi.mapper

import org.horizontal.tella.mobile.data.entity.uwazi.CommonPropertyEntity
import org.horizontal.tella.mobile.data.entity.uwazi.DictionaryResponse
import org.horizontal.tella.mobile.data.entity.uwazi.NestedValueEntity
import org.horizontal.tella.mobile.data.entity.uwazi.PropertyEntity
import org.horizontal.tella.mobile.data.entity.uwazi.RelationShipEntitiesResponse
import org.horizontal.tella.mobile.data.entity.uwazi.RowDictionaryEntity
import org.horizontal.tella.mobile.data.entity.uwazi.TemplateResponse
import org.horizontal.tella.mobile.data.entity.uwazi.UwaziEntityRow
import org.horizontal.tella.mobile.data.entity.uwazi.UwaziRelationShipRow
import org.horizontal.tella.mobile.data.entity.uwazi.Value
import org.horizontal.tella.mobile.data.entity.uwazi.ValueEntity
import org.horizontal.tella.mobile.domain.entity.uwazi.CommonProperty
import org.horizontal.tella.mobile.domain.entity.uwazi.NestedSelectValue
import org.horizontal.tella.mobile.domain.entity.uwazi.Property
import org.horizontal.tella.mobile.domain.entity.uwazi.RelationShipRow
import org.horizontal.tella.mobile.domain.entity.uwazi.RowDictionary
import org.horizontal.tella.mobile.domain.entity.uwazi.SelectValue
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziRow

/**
 * Mapper for template response
 */
fun RelationShipEntitiesResponse.mapToDomainModel() = rows?.map {
    it.mapToDomainModel()
} ?: emptyList()

fun TemplateResponse.mapToDomainModel() = rows?.map {
    it.mapToDomainModel()
} ?: emptyList()

fun UwaziRelationShipRow.mapToDomainModel() = RelationShipRow(
    version = __v,
    id = _id,
    values = values?.map { it.mapToDomainModel() } ?: emptyList(),
    type = type,
    name = name,
    entityViewPage = entityViewPage
)

fun Value.mapToDomainModel() = org.horizontal.tella.mobile.domain.entity.uwazi.Value(
    label = label,
    id = id
)

fun UwaziEntityRow.mapToDomainModel() = UwaziRow(
    version = __v,
    _id = _id,
    commonProperties = commonProperties?.map { it.mapToDomainModel() } ?: emptyList(),
    default = default,
    name = name,
    properties = properties?.map { it.mapToDomainModel() } ?: emptyList())

fun CommonPropertyEntity.mapToDomainModel() = CommonProperty(
    id = _id,
    generatedId = generatedId,
    isCommonProperty = isCommonProperty,
    label = label,
    localID = localID ?: "",
    name = name,
    prioritySorting = prioritySorting,
    type = type
)

fun PropertyEntity.mapToDomainModel() = Property(
    _id = _id ?: "",
    id = id ?: "",
    showInCard = showInCard,
    content = content ?: "",
    label = label ?: "",
    name = name ?: "",
    nestedProperties = nestedProperties ?: emptyList(),
    required = required,
    type = type
)

/**
 * Mapper for dictionary response
 */
fun DictionaryResponse.mapToDomainModel() = rows?.map {
    it.mapToDomainModel()
} ?: emptyList()

fun RowDictionaryEntity.mapToDomainModel() = RowDictionary(
    version = __v ?: 0,
    _id = _id ?: "",
    name = name ?: "",
    values = values?.map { it.mapToDomainModel() } ?: emptyList()
)

fun ValueEntity.mapToDomainModel() = SelectValue(
    _id = _id ?: "",
    id = id ?: "",
    label = label,
    values = values?.map { it.mapToDomainModel() } ?: emptyList()
)

fun NestedValueEntity.mapToDomainModel() = NestedSelectValue(
    id = id ?: "",
    label = label ?: ""
)

