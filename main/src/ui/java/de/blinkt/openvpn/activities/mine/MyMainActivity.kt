/*
 * Copyright (c) 2012-2024 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities.mine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import de.blinkt.openvpn.R
import de.blinkt.openvpn.theme.NavigationDrawerComposeTheme
import kotlinx.coroutines.launch

class MyMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
//        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MyMainActivity()
        }
    }

    @Preview
    @Composable
    fun MyMainActivity() {
        NavigationDrawerComposeTheme {
            val systemUiController = rememberSystemUiController()
            val scaffoldState = rememberScaffoldState()
            val scope = rememberCoroutineScope()
            systemUiController.setStatusBarColor(Color(0xFF2F2B37))
            Scaffold (
                scaffoldState = scaffoldState,
                drawerScrimColor = Color(0x31000000),
                topBar = {
                    AppBar (
                        onNavigationClick = {
                            scope.launch {
                                scaffoldState.drawerState.open()
                            }
                        }
                    )
                },
                drawerContent = {
                    DrawerHeader()
                    DrawerBody(items = listOf(
                        MenuItem(
                            id = "home",
                            title = "Home",
                            contentDescription = "Home",
                            icon = ImageVector.vectorResource(R.drawable.home)
                        ),
                        MenuItem(
                            id = "statistics",
                            title = "Statistics",
                            contentDescription = "Statistics",
                            icon = ImageVector.vectorResource(R.drawable.stats)
                        ),
                        MenuItem(
                            id = "settings",
                            title = "Settings",
                            contentDescription = "Settings",
                            icon = ImageVector.vectorResource(R.drawable.settings)
                        ),
                        MenuItem(
                            id = "faq",
                            title = "FAQ",
                            contentDescription = "FAQ",
                            icon = ImageVector.vectorResource(R.drawable.faq)
                        ),
                        MenuItem(
                            id = "about",
                            title = "About",
                            contentDescription = "About",
                            icon = ImageVector.vectorResource(R.drawable.about)
                        )
                    ),
                        onItemClick = {
                            println("clicked on ${it.title}")
                        }
                    )
                }
            ) {

            }
        }
    }
}