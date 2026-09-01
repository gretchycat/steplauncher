package com.steplauncher.app

import android.app.Presentation
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.Display
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.steplauncher.app.databinding.ActivityLauncherBinding
import com.steplauncher.core.renderer.DockManager
import com.steplauncher.core.renderer.DockPosition

/**
 * Bossy Monitor Presentation class for displaying the StepLauncher desktop system on secondary displays.
 * All monitors run from the same pool of workspaces, with each monitor having its own independent active workspace selection.
 */
class StepLauncherPresentation(
    context: Context,
    display: Display
) : Presentation(context, display) {

    private lateinit var binding: ActivityLauncherBinding
    val displayIdInt: Int = display.displayId
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private val dockChangeListener: () -> Unit = {
        mainHandler.post {
            refreshDocks()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        setupGestureDetectors()
        DockManager.addChangeListener(dockChangeListener)
        refreshDocks()
    }

    override fun onStop() {
        super.onStop()
        DockManager.removeChangeListener(dockChangeListener)
    }

    private fun setupRecyclerViews() {
        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        binding.rvDockBottomLeft.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true).apply {
            stackFromEnd = true
        }
        binding.rvDockTopRight.layoutManager = LinearLayoutManager(context, if (isPortrait) LinearLayoutManager.HORIZONTAL else LinearLayoutManager.VERTICAL, false)
        binding.rvDockBottom.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun setupGestureDetectors() {
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 120 && Math.abs(velocityX) > 200) {
                    if (diffX < 0) {
                        DockManager.nextWorkspaceForDisplay(displayIdInt, context)
                        val currWs = DockManager.getWorkspaceIndexForDisplay(displayIdInt) + 1
                        Toast.makeText(context, "❖ Display $displayIdInt Workspace $currWs of ${DockManager.totalWorkspaces}", Toast.LENGTH_SHORT).show()
                    } else {
                        DockManager.prevWorkspaceForDisplay(displayIdInt, context)
                        val currWs = DockManager.getWorkspaceIndexForDisplay(displayIdInt) + 1
                        Toast.makeText(context, "❖ Display $displayIdInt Workspace $currWs of ${DockManager.totalWorkspaces}", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
                return false
            }
        })

        binding.root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
    }

    fun refreshDocks() {
        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val displayMetrics = context.resources.displayMetrics
        val screenWidthPx = displayMetrics.widthPixels
        val screenHeightPx = displayMetrics.heightPixels

        val activeWorkspaceTiles = DockManager.getTopRightDockTilesForDisplay(displayIdInt)

        // Bottom-Left Dock: Running Tasks Stack
        val hasRunningTasks = DockManager.bottomLeftDockTiles.isNotEmpty()
        binding.dockBottomLeftContainer.visibility = if (hasRunningTasks) View.VISIBLE else View.GONE
        binding.rvDockBottomLeft.adapter = DockTileAdapter(
            tiles = DockManager.bottomLeftDockTiles,
            onTileClick = { tile -> (context as? LauncherActivity)?.handleTileClick(tile, DockPosition.BOTTOM_LEFT) },
            onTileLongClickMenu = { tile, v -> (context as? LauncherActivity)?.handleTileLongClickMenu(tile, DockPosition.BOTTOM_LEFT) }
        )

        // Right Dock (Workspace Dock): Auto resizes items into the number of items in the right dock (landscape)
        binding.rvDockTopRight.adapter = DockTileAdapter(
            tiles = activeWorkspaceTiles,
            isRightDock = true,
            isPortrait = isPortrait,
            parentContainerWidthPx = screenWidthPx,
            parentContainerHeightPx = screenHeightPx,
            onTileClick = { tile -> (context as? LauncherActivity)?.handleTileClick(tile, DockPosition.TOP_RIGHT) },
            onTileLongClickMenu = { tile, v -> (context as? LauncherActivity)?.handleTileLongClickMenu(tile, DockPosition.TOP_RIGHT) }
        )

        // Bottom Dock (Global Dock): Scales to fill the complete width in portrait mode
        binding.rvDockBottom.adapter = DockTileAdapter(
            tiles = DockManager.bottomDockTiles,
            isBottomDock = true,
            isPortrait = isPortrait,
            parentContainerWidthPx = screenWidthPx,
            parentContainerHeightPx = screenHeightPx,
            onTileClick = { tile -> (context as? LauncherActivity)?.handleTileClick(tile, DockPosition.BOTTOM) },
            onTileLongClickMenu = { tile, v -> (context as? LauncherActivity)?.handleTileLongClickMenu(tile, DockPosition.BOTTOM) }
        )
    }
}
