package org.stickynodes.editor.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.draw.geometry.selection.NodeAlignment
import org.stickynodes.editor.draw.geometry.selection.NodeDistribution
import org.stickynodes.editor.draw.geometry.selection.NodeSizing
import org.stickynodes.editor.tools.components.ActionBarButton

@Composable
fun LayoutBar(
    onAlign: (NodeAlignment) -> Unit,
    onDistribute: (NodeDistribution) -> Unit,
    canDistribute: Boolean,
    onMatchSize: (NodeSizing) -> Unit,
    canMatchSize: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            
            LayoutButtonRow {
                ActionBarButton(
                    icon = "align_horizontal_left",
                    tooltip = "Align left",
                    onClick = { onAlign(NodeAlignment.LEFT) }
                )

                ActionBarButton(
                    icon = "align_horizontal_center",
                    tooltip = "Align horizontal centres",
                    onClick = { onAlign(NodeAlignment.HORIZONTAL_CENTER) },
                )

                ActionBarButton(
                    icon = "align_horizontal_right",
                    tooltip = "Align right",
                    onClick = { onAlign(NodeAlignment.RIGHT) },
                )
            }

            LayoutButtonRow {
                ActionBarButton(
                    icon = "align_vertical_top",
                    tooltip = "Align top",
                    onClick = { onAlign(NodeAlignment.TOP) },
                )

                ActionBarButton(
                    icon = "align_vertical_center",
                    tooltip = "Align vertical centres",
                    onClick = { onAlign(NodeAlignment.VERTICAL_CENTER) },
                )

                ActionBarButton(
                    icon = "align_vertical_bottom",
                    tooltip = "Align bottom",
                    onClick = { onAlign(NodeAlignment.BOTTOM) },
                )
            }


            HorizontalDivider(
                modifier = Modifier.width(72.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            LayoutButtonRow {
                ActionBarButton(
                    icon = "horizontal_distribute",
                    tooltip = "Distribute horizontally",
                    onClick = { onDistribute(NodeDistribution.HORIZONTAL) },
                    enabled = canDistribute
                )

                ActionBarButton(
                    icon = "vertical_distribute",
                    tooltip = "Distribute vertically",
                    onClick = { onDistribute(NodeDistribution.VERTICAL) },
                    enabled = canDistribute
                )
            }

            HorizontalDivider(
                modifier = Modifier.width(72.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            LayoutButtonRow {
                ActionBarButton(
                    icon = "width",
                    tooltip = "Match widths",
                    enabled = canMatchSize,
                    onClick = {
                        onMatchSize(NodeSizing.MATCH_WIDTH)
                    },
                )

                ActionBarButton(
                    icon = "height",
                    tooltip = "Match heights",
                    enabled = canMatchSize,
                    onClick = {
                        onMatchSize(NodeSizing.MATCH_HEIGHT)
                    },
                )

                ActionBarButton(
                    icon = "aspect_ratio",
                    tooltip = "Match size",
                    enabled = canMatchSize,
                    onClick = {
                        onMatchSize(NodeSizing.MATCH_BOTH)
                    },
                )

            }
        }
    }
}


@Composable
private fun LayoutButtonRow(
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}


@Preview
@Composable
private fun LayoutBarPreview() {
    MaterialTheme {
        LayoutBar(
            onAlign = {},
            onDistribute = {},
            canDistribute = false,
            onMatchSize = {},
            canMatchSize = true
        )
    }
}