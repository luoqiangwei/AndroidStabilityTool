package com.ovea_y.stabilitytool

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 测试状态
 */
enum class TestStatus {
    IDLE,       // 空闲状态
    RUNNING,    // 测试运行中
    SUCCESS,    // 测试成功
    WARNING,    // 测试警告
    ERROR       // 测试错误
}

/**
 * 测试结果显示组件
 *
 * @param title 测试标题
 * @param status 测试状态
 * @param progress 测试进度 (0.0f - 1.0f)
 * @param message 测试消息
 * @param details 测试详细信息
 * @param modifier Modifier
 */
@Composable
fun TestResultDisplay(
    title: String,
    status: TestStatus,
    progress: Float = 0f,
    message: String = "",
    details: String = "",
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "ProgressAnimation"
    )
    
    val (statusIcon, statusColor) = when (status) {
        TestStatus.IDLE -> Pair(null, MaterialTheme.colorScheme.surfaceVariant)
        TestStatus.RUNNING -> Pair(null, MaterialTheme.colorScheme.primary)
        TestStatus.SUCCESS -> Pair(Icons.Filled.Check, MaterialTheme.colorScheme.primary)
        TestStatus.WARNING -> Pair(Icons.Filled.Warning, MaterialTheme.colorScheme.tertiary)
        TestStatus.ERROR -> Pair(Icons.Filled.Warning, MaterialTheme.colorScheme.error)
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 状态图标或进度指示器
                if (status == TestStatus.RUNNING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = statusColor
                    )
                } else if (statusIcon != null) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = status.name,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // 标题
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(start = if (status == TestStatus.IDLE) 0.dp else 8.dp)
                        .weight(1f)
                )
            }
            
            // 进度条
            AnimatedVisibility(
                visible = status == TestStatus.RUNNING && progress > 0f,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth(),
                        strokeCap = StrokeCap.Round,
                        color = statusColor,
                        trackColor = statusColor.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
            
            // 消息
            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // 详细信息
            if (details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = stringResource(R.string.details_info),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Text(
                            text = details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(start = 4.dp),
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
} 