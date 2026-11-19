-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `mydb` DEFAULT CHARACTER SET utf8 ;
USE `mydb` ;

-- -----------------------------------------------------
-- Table `mydb`.`departamento`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`departamento` (
  `Código` INT NOT NULL,
  `Nombre` VARCHAR(45) NULL,
  `Ciudad` VARCHAR(45) NULL,
  PRIMARY KEY (`Código`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `mydb`.`Persona`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`Persona` (
  `cedula` INT NOT NULL,
  `nombre` VARCHAR(45) NULL,
  PRIMARY KEY (`cedula`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `mydb`.`profesor`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`profesor` (
  `cedula` INT NOT NULL,
  `título` VARCHAR(45) NULL,
  `salario` DECIMAL(12) NULL,
  `departamentos_Código` INT NOT NULL,
  `Persona_cedula` INT NOT NULL,
  PRIMARY KEY (`cedula`, `departamentos_Código`, `Persona_cedula`),
  INDEX `fk_profesor_departamentos1_idx` (`departamentos_Código` ASC) VISIBLE,
  INDEX `fk_profesor_Persona1_idx` (`Persona_cedula` ASC) VISIBLE,
  CONSTRAINT `fk_profesor_departamentos1`
    FOREIGN KEY (`departamentos_Código`)
    REFERENCES `mydb`.`departamento` (`Código`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_profesor_Persona1`
    FOREIGN KEY (`Persona_cedula`)
    REFERENCES `mydb`.`Persona` (`cedula`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `mydb`.`curso`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`curso` (
  `código` INT NOT NULL,
  `nombre` VARCHAR(45) NULL,
  `número de créditos` INT NULL,
  `departamentos_Código` INT NOT NULL,
  `profesor_Identificador` INT NOT NULL,
  PRIMARY KEY (`código`, `departamentos_Código`, `profesor_Identificador`),
  INDEX `fk_curso_departamentos1_idx` (`departamentos_Código` ASC) VISIBLE,
  INDEX `fk_curso_profesor1_idx` (`profesor_Identificador` ASC) VISIBLE,
  CONSTRAINT `fk_curso_departamentos1`
    FOREIGN KEY (`departamentos_Código`)
    REFERENCES `mydb`.`departamento` (`Código`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_curso_profesor1`
    FOREIGN KEY (`profesor_Identificador`)
    REFERENCES `mydb`.`profesor` (`cedula`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `mydb`.`estudiante`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`estudiante` (
  `cedula` INT NOT NULL,
  `fecha de nacimiento` DATETIME NULL,
  `ciudad` VARCHAR(45) NULL,
  `Persona_cedula` INT NOT NULL,
  PRIMARY KEY (`cedula`, `Persona_cedula`),
  INDEX `fk_estudiante_Persona1_idx` (`Persona_cedula` ASC) VISIBLE,
  CONSTRAINT `fk_estudiante_Persona1`
    FOREIGN KEY (`Persona_cedula`)
    REFERENCES `mydb`.`Persona` (`cedula`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `mydb`.`Estudiante en el curso`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`Estudiante en el curso` (
  `estudiante_código` INT NOT NULL,
  `curso_código` INT NOT NULL,
  `Semestre` VARCHAR(20) NULL,
  `Nota` DECIMAL(3,2) NULL,
  PRIMARY KEY (`estudiante_código`, `curso_código`),
  INDEX `fk_estudiante_has_curso_curso1_idx` (`curso_código` ASC) VISIBLE,
  INDEX `fk_estudiante_has_curso_estudiante1_idx` (`estudiante_código` ASC) VISIBLE,
  CONSTRAINT `fk_estudiante_has_curso_estudiante1`
    FOREIGN KEY (`estudiante_código`)
    REFERENCES `mydb`.`estudiante` (`código`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_estudiante_has_curso_curso1`
    FOREIGN KEY (`curso_código`)
    REFERENCES `mydb`.`curso` (`código`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `mydb`.`estudiante`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`estudiante` (
  `cedula` INT NOT NULL,
  `fecha de nacimiento` DATETIME NULL,
  `ciudad` VARCHAR(45) NULL,
  `Persona_cedula` INT NOT NULL,
  PRIMARY KEY (`cedula`, `Persona_cedula`),
  INDEX `fk_estudiante_Persona1_idx` (`Persona_cedula` ASC) VISIBLE,
  CONSTRAINT `fk_estudiante_Persona1`
    FOREIGN KEY (`Persona_cedula`)
    REFERENCES `mydb`.`Persona` (`cedula`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `mydb`.`Evaluación`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`Evaluación` (
  `Id` INT NOT NULL AUTO_INCREMENT,
  `curso_código` INT NOT NULL,
  `Temas` VARCHAR(45) NULL,
  PRIMARY KEY (`Id`, `curso_código`),
  INDEX `fk_profesor_has_curso_curso1_idx` (`curso_código` ASC) VISIBLE,
  CONSTRAINT `fk_profesor_has_curso_curso1`
    FOREIGN KEY (`curso_código`)
    REFERENCES `mydb`.`curso` (`código`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
