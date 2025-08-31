// MainActivity.kt
package com.example.spacedefender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpaceDefenderTheme {
                GameScreen()
            }
        }
    }
}

@Composable
fun SpaceDefenderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content
    )
}

data class Player(
    var x: Float = 0f,
    var y: Float = 0f,
    val size: Float = 40f
)

data class Enemy(
    var x: Float,
    var y: Float,
    val size: Float = 30f,
    var isAlive: Boolean = true
)

data class Bullet(
    var x: Float,
    var y: Float,
    val size: Float = 8f,
    var isActive: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen() {
    val density = LocalDensity.current
    var gameState by remember { mutableStateOf("menu") } // "menu", "playing", "gameOver"
    var score by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(0) }
    
    var player by remember { mutableStateOf(Player()) }
    var enemies by remember { mutableStateOf(listOf<Enemy>()) }
    var bullets by remember { mutableStateOf(listOf<Bullet>()) }
    var screenWidth by remember { mutableFloatStateOf(0f) }
    var screenHeight by remember { mutableFloatStateOf(0f) }
    
    // Game loop
    LaunchedEffect(gameState) {
        if (gameState == "playing") {
            while (gameState == "playing") {
                delay(16) // ~60 FPS
                
                // Move bullets
                bullets = bullets.map { bullet ->
                    bullet.copy(y = bullet.y - 10f)
                }.filter { it.y > 0 }
                
                // Move enemies
                enemies = enemies.map { enemy ->
                    enemy.copy(y = enemy.y + 3f)
                }
                
                // Remove off-screen enemies and check game over
                enemies = enemies.filter { enemy ->
                    if (enemy.y > screenHeight) {
                        false // Remove enemy
                    } else if (enemy.y + enemy.size > player.y && 
                              abs(enemy.x - player.x) < (enemy.size + player.size) / 2) {
                        // Collision with player
                        if (score > highScore) highScore = score
                        gameState = "gameOver"
                        true
                    } else {
                        true
                    }
                }
                
                // Check bullet-enemy collisions
                val bulletsToRemove = mutableSetOf<Int>()
                val enemiesToRemove = mutableSetOf<Int>()
                
                bullets.forEachIndexed { bulletIndex, bullet ->
                    enemies.forEachIndexed { enemyIndex, enemy ->
                        if (abs(bullet.x - enemy.x) < (bullet.size + enemy.size) / 2 &&
                            abs(bullet.y - enemy.y) < (bullet.size + enemy.size) / 2) {
                            bulletsToRemove.add(bulletIndex)
                            enemiesToRemove.add(enemyIndex)
                            score += 10
                        }
                    }
                }
                
                bullets = bullets.filterIndexed { index, _ -> index !in bulletsToRemove }
                enemies = enemies.filterIndexed { index, _ -> index !in enemiesToRemove }
                
                // Spawn new enemies randomly
                if (Random.nextFloat() < 0.02f && enemies.size < 8) {
                    enemies = enemies + Enemy(
                        x = Random.nextFloat() * (screenWidth - 30f),
                        y = -30f
                    )
                }
            }
        }
    }
    
    when (gameState) {
        "menu" -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🚀 SPACE DEFENDER",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Cyan
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "High Score: $highScore",
                    fontSize = 18.sp,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Button(
                    onClick = {
                        gameState = "playing"
                        score = 0
                        enemies = listOf()
                        bullets = listOf()
                        player = Player()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan)
                ) {
                    Text("START GAME", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Drag to move • Tap to shoot\nAvoid enemies • Destroy them for points!",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
            }
        }
        
        "playing" -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                player = player.copy(
                                    x = (player.x + dragAmount.x).coerceIn(
                                        0f, 
                                        screenWidth - player.size
                                    )
                                )
                            }
                        }
                        .pointerInput(Unit) {
                            androidx.compose.foundation.gestures.detectTapGestures {
                                // Shoot bullet
                                bullets = bullets + Bullet(
                                    x = player.x + player.size / 2,
                                    y = player.y
                                )
                            }
                        }
                ) {
                    screenWidth = size.width
                    screenHeight = size.height
                    
                    // Initialize player position
                    if (player.x == 0f && player.y == 0f) {
                        player = player.copy(
                            x = screenWidth / 2 - player.size / 2,
                            y = screenHeight - 100f
                        )
                    }
                    
                    // Draw stars background
                    repeat(50) {
                        drawCircle(
                            color = Color.White,
                            radius = 1f,
                            center = Offset(
                                Random.nextFloat() * screenWidth,
                                Random.nextFloat() * screenHeight
                            )
                        )
                    }
                    
                    // Draw player
                    drawPlayer(player)
                    
                    // Draw enemies
                    enemies.forEach { enemy ->
                        drawEnemy(enemy)
                    }
                    
                    // Draw bullets
                    bullets.forEach { bullet ->
                        drawBullet(bullet)
                    }
                }
                
                // Score display
                Text(
                    text = "Score: $score",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                )
                
                // Pause button
                Button(
                    onClick = { gameState = "menu" },
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f))
                ) {
                    Text("⏸", fontSize = 16.sp)
                }
            }
        }
        
        "gameOver" -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "💥 GAME OVER",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Final Score: $score",
                    fontSize = 24.sp,
                    color = Color.White
                )
                
                if (score == highScore && score > 0) {
                    Text(
                        text = "🎉 NEW HIGH SCORE!",
                        fontSize = 18.sp,
                        color = Color.Yellow,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            gameState = "playing"
                            score = 0
                            enemies = listOf()
                            bullets = listOf()
                            player = Player()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                    ) {
                        Text("PLAY AGAIN", fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = { gameState = "menu" },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("MENU", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun DrawScope.drawPlayer(player: Player) {
    // Draw spaceship body (triangle)
    val path = androidx.compose.ui.graphics.Path()
    path.moveTo(player.x + player.size / 2, player.y)
    path.lineTo(player.x, player.y + player.size)
    path.lineTo(player.x + player.size, player.y + player.size)
    path.close()
    
    drawPath(
        path = path,
        color = Color.Cyan
    )
    
    // Draw engine glow
    drawCircle(
        color = Color.Yellow,
        radius = 8f,
        center = Offset(player.x + player.size / 2, player.y + player.size + 5f)
    )
}

fun DrawScope.drawEnemy(enemy: Enemy) {
    // Draw enemy as red diamond
    val path = androidx.compose.ui.graphics.Path()
    path.moveTo(enemy.x + enemy.size / 2, enemy.y)
    path.lineTo(enemy.x + enemy.size, enemy.y + enemy.size / 2)
    path.lineTo(enemy.x + enemy.size / 2, enemy.y + enemy.size)
    path.lineTo(enemy.x, enemy.y + enemy.size / 2)
    path.close()
    
    drawPath(
        path = path,
        color = Color.Red
    )
}

fun DrawScope.drawBullet(bullet: Bullet) {
    drawCircle(
        color = Color.Yellow,
        radius = bullet.size,
        center = Offset(bullet.x, bullet.y)
    )
}