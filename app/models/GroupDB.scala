package models

/**
 * Created by kyota.yasuda on 15/08/20.
 */
case class GroupDB(id: Long,
                   users: Set[Long],
                   created_at: String)
