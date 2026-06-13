package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ActiveTab
import com.example.ui.PosViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppLayout(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(viewModel: PosViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Observe toast messages from view model
    LaunchedEffect(key1 = true) {
        viewModel.toastMessage.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Modal Drawer for Sidebar Navigation Menu
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                // Brand Logo Header card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "AL ILM BOOK DEPOT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "POS & Inventory Terminal",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Navigation Items
                val menuItems = listOf(
                    Triple(ActiveTab.Dashboard, Icons.Default.Home, "Dashboard"),
                    Triple(ActiveTab.POS, Icons.Default.ShoppingCart, "Sale POS"),
                    Triple(ActiveTab.Products, Icons.Default.List, "Product Stock"),
                    Triple(ActiveTab.Purchases, Icons.Default.Add, "Stock Purchases"),
                    Triple(ActiveTab.Reports, Icons.Default.List, "Reports & Sales"),
                    Triple(ActiveTab.Settings, Icons.Default.Settings, "System Settings")
                )

                menuItems.forEach { (tab, icon, label) ->
                    NavigationDrawerItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                        selected = currentTab == tab,
                        onClick = {
                            viewModel.selectTab(tab)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                            .testTag("drawer_item_${tab.toString().lowercase()}")
                    )
                }
            }
        }
    ) {
        // Main Screen Frame Scaffold
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Sidebar Menu")
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = "AL ILM BOOK DEPOT",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = when (currentTab) {
                                    ActiveTab.Dashboard -> "Enterprise Dashboard summary"
                                    ActiveTab.POS -> "Point of Sale (Terminal)"
                                    ActiveTab.Products -> "Product Directory & stock info"
                                    ActiveTab.Purchases -> "Add product purchases log"
                                    ActiveTab.Reports -> "Income Ledger reports"
                                    ActiveTab.Settings -> "Terminal backups & details"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    },
                    actions = {
                        // Icon indicators
                        IconButton(onClick = { viewModel.selectTab(ActiveTab.POS) }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "POS checkout shortcut")
                        }
                        IconButton(onClick = { viewModel.selectTab(ActiveTab.Settings) }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Store profile settings")
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                )
            },
            bottomBar = {
                // Universal Footer layout displaying "MADE WITH ❤️ BY ARFI"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .navigationBarsPadding()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MADE WITH ❤️ BY ARFI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    ActiveTab.Dashboard -> DashboardScreen(viewModel)
                    ActiveTab.POS -> SalesScreen(viewModel)
                    ActiveTab.Products -> ProductManagementScreen(viewModel)
                    ActiveTab.Purchases -> PurchaseScreen(viewModel)
                    ActiveTab.Reports -> ReportsScreen(viewModel)
                    ActiveTab.Settings -> SettingsScreen(viewModel)
                }
            }
        }
    }
}
