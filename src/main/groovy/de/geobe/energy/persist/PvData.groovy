package de.geobe.energy.persist
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class PvData {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long id
    Long recordedAt
    Integer prodAvg = 0
    Integer prodMin = 0
    Integer prodMax = 0
    Integer consAvg = 0
    Integer consMin = 0
    Integer consMax = 0
    Integer gridAvg = 0
    Integer gridMin = 0
    Integer gridMax = 0
    Integer toGrid = 0
    Integer fromGrid = 0
    Integer battAvg = 0
    Integer battMin = 0
    Integer battMax = 0
    Integer battLoad = 0
}
