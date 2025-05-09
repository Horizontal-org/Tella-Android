package org.horizontal.tella.mobile.domain.entity.uwazi

import org.horizontal.tella.mobile.presentation.uwazi.UwaziRelationShipEntity

data class Property(val _id: String,
                    val content: String,
                    val id: String,
                    val label: String,
                    var translatedLabel : String = "",
                    val name: String,
                    val nestedProperties: List<Any>,
                    val required: Boolean,
                    val showInCard: Boolean,
                    val type: String,
                    var entities : List<Value> ? = null,
                    var selectedEntities : List<UwaziRelationShipEntity> ? = null,
                    var values : List<SelectValue>? = null)
