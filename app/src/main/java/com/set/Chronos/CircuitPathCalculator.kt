package com.set.Chronos

import androidx.compose.ui.geometry.Offset

data class AlarmPath(
    val alarm: CircuitAlarmData,
    val points: List<Offset>,
    val extraPoint: Offset?,
    val mergePoint: Offset?
)

object CircuitPathCalculator {

    fun buildAlarmPath(
        a: CircuitAlarmData,
        yKeyframes: List<Float>,
        activeAtY: Map<Float, List<CircuitAlarmData>>,
        startYMap: Map<CircuitAlarmData, Float>,
        endYMap: Map<CircuitAlarmData, Float>,
        allAlarms: List<CircuitAlarmData>,
        centerX: Float,
        spacing: Float
    ): AlarmPath {
        val myYPoints = yKeyframes.filter { y -> y >= (startYMap[a] ?: 0f) - 1f && y <= (endYMap[a] ?: 0f) + 1f }
        val firstY = myYPoints.firstOrNull()
        val lastY = myYPoints.lastOrNull()

        // ── prevY: 이 알람이 시작할 때 같은 taskLine에 시간 겹침 알람이 있으면 이전 keyframe 탐색 ──
        val prevY: Float? = if (firstY != null) {
            val sameLine = activeAtY[firstY]?.filter { it.alarm.taskLine == a.alarm.taskLine } ?: emptyList()
            val timeOverlap = sameLine.filter { other -> a.start < other.end && a.end > other.start }
            if (timeOverlap.size > 1) yKeyframes.lastOrNull { it <= firstY - 1f } else null
        } else null

        // ── extraPoint: 뿌리에서 갈라지기 직전 합류 좌표 ──
        // 🔥 torch-pass: 같은 taskLine에서 a.start에 끝나는 전임 알람의 실제 x 위치 사용 (X자 교차 방지)
        val extraPoint: Offset? = prevY?.let { py ->
            val active = activeAtY[py] ?: emptyList()
            val activeLines = active.map { it.alarm.taskLine }.distinct().sorted()
            val lineBaseX = centerX + (activeLines.indexOf(a.alarm.taskLine) - (activeLines.size - 1) / 2f) * spacing

            val predecessor = active.firstOrNull { other ->
                other.alarm.taskLine == a.alarm.taskLine && other.end == a.start
            }

            val extraX = if (predecessor != null) {
                val sameLineActive = active.filter { it.alarm.taskLine == a.alarm.taskLine }.sortedBy { it.start }
                val timeOverlapping = sameLineActive.filter { other ->
                    predecessor.start < other.end && predecessor.end > other.start
                }
                val count = timeOverlapping.size
                val idx = timeOverlapping.indexOf(predecessor)
                val sub = spacing / maxOf(count, 1)
                if (count <= 1) lineBaseX else lineBaseX + (idx - (count - 1) / 2f) * sub
            } else {
                lineBaseX
            }
            Offset(extraX, py)
        }

        // ── mergePoint: 알람이 끝난 뒤 메인 라인으로 합류하는 좌표 ──
        // 🔥 torch-pass: 후임 알람이 있으면 mergePoint 불필요 (X자 교차 방지)
        val nextY: Float? = if (lastY != null) yKeyframes.firstOrNull { it >= lastY + 1f } else null

        val hasTorchSuccessor = allAlarms.any { other ->
            other !== a && other.alarm.taskLine == a.alarm.taskLine && other.start == a.end
        }

        val mergePoint: Offset? = if (hasTorchSuccessor) {
            null
        } else if (lastY != null && nextY != null) {
            val atLast = activeAtY[lastY]?.filter { it.alarm.taskLine == a.alarm.taskLine } ?: emptyList()
            val overlapAtLast = atLast.filter { other -> a.start < other.end && a.end > other.start }
            val atNext = activeAtY[nextY]?.filter { it.alarm.taskLine == a.alarm.taskLine } ?: emptyList()
            val overlapAtNext = atNext.filter { other -> a.start < other.end && a.end > other.start }

            if (overlapAtLast.size > 1 && overlapAtNext.isNotEmpty()) {
                val activeLines = (activeAtY[nextY] ?: emptyList())
                    .map { it.alarm.taskLine }.distinct().sorted()
                val homeX = centerX + (activeLines.indexOf(a.alarm.taskLine) - (activeLines.size - 1) / 2f) * spacing
                Offset(homeX, nextY)
            } else null
        } else null

        // ── points: 알람의 메인 좌표 목록 ──
        val points = myYPoints.map { y ->
            val active = activeAtY[y] ?: emptyList()
            val activeLines = active.map { it.alarm.taskLine }.distinct().sorted()
            val lineBaseX = centerX + (activeLines.indexOf(a.alarm.taskLine) - (activeLines.size - 1) / 2f) * spacing

            val sameLineActive = active.filter { it.alarm.taskLine == a.alarm.taskLine }.sortedBy { it.start }
            val timeOverlapping = sameLineActive.filter { other ->
                a.start < other.end && a.end > other.start
            }
            val count = timeOverlapping.size
            val idx = timeOverlapping.indexOf(a)
            val sub = spacing / maxOf(count, 1)
            val branchX = if (count <= 1) lineBaseX else lineBaseX + (idx - (count - 1) / 2f) * sub

            Offset(branchX, y)
        }

        return AlarmPath(a, points, extraPoint, mergePoint)
    }
}