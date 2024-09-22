package com.ovea_y.stabilitytool

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ovea_y.stabilitytool.ui.theme.ButtonCommonPadding
import com.ovea_y.stabilitytool.ui.theme.ColumnCommonPadding
import com.ovea_y.stabilitytool.ui.theme.GlobalCommonPadding
import com.ovea_y.stabilitytool.ui.theme.RoundedCornerShapeCommon
import com.ovea_y.stabilitytool.ui.theme.StabilityToolTheme

@Composable
fun HomePage(navController: NavHostController) {
    val scrollState = rememberScrollState()

    Scaffold() { contentPadding ->
        Column(modifier = Modifier
            .padding(contentPadding)
            .padding(all = GlobalCommonPadding)
            .fillMaxSize()
            .verticalScroll(scrollState)) {
            Text(
                stringResource(id = R.string.home_page_title),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center)
            Text(
                stringResource(id = R.string.home_tips),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Left)
            Spacer(modifier = Modifier.height(GlobalCommonPadding))
            Row() {
                Column(modifier = Modifier
                    .padding(all = ColumnCommonPadding)
                    .fillMaxSize(0.5f)) {

                    // ANR Category
                    CommonButton(text = stringResource(R.string.anr_test)) {
                        navController.navigate("anr")
                    }

                    // Crash Category
                    CommonButton(text = stringResource(R.string.crash_test)) {

                    }

                    // Cpu Performance Category
                    CommonButton(text = stringResource(R.string.cpu_test)) {

                    }

                    // Disk I/O Category
                    CommonButton(text = stringResource(R.string.disk_test)) {

                    }
                }
                Column(modifier = Modifier
                    .padding(all = ColumnCommonPadding)
                    .fillMaxSize()) {

                    CommonButton(text = stringResource(R.string.network_test)) { }

                    CommonButton(text = stringResource(R.string.memory_test)) { }

                    CommonButton(text = stringResource(R.string.power_test)) { }
                }
            }
        }
    }
}

@Composable
fun CommonButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = ButtonCommonPadding),
        shape = RoundedCornerShape(RoundedCornerShapeCommon)
    ) {
        Text(text)
    }
}

@Preview(showBackground = true)
@Composable
fun HomePagePreview() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomePage(navController) }
    }

    StabilityToolTheme {
        HomePage(navController)
    }
}