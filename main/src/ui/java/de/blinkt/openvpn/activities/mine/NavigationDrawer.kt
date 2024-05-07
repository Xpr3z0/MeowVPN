/*
 * Copyright (c) 2012-2024 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun DrawerHeader() {
    Box (
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2F2B37))
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,

    ) {
        Text(
            text = "Header",
            fontSize = 60.sp,
            color = Color.White
        )
    }
}
@Composable
fun DrawerBody(
    items: List<MenuItem>,
    modifier: Modifier = Modifier
        .background(Color(0xFF282531))
        .fillMaxHeight(),
    itemTextStyle: TextStyle = TextStyle(fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold),
    onItemClick: (MenuItem) -> Unit
) {
    LazyColumn(modifier) {
        items(items) { item ->
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onItemClick(item)
                    }
                    .padding(20.dp, 16.dp)
                    .background(Color(0xFF282531))
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.contentDescription
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = item.title,
                    style = itemTextStyle,

                    modifier = Modifier.weight(1f) // будет занимать всё оставшееся место
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}