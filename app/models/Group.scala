package models

import java.time.LocalDateTime

/**
 * Created by kyota.yasuda on 15/08/20.
 */
case class Group(id: Long,
                 users: Set[Long],
                  created_at: LocalDateTime)
