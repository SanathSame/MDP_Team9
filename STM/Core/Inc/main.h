/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : main.h
  * @brief          : Header for main.c file.
  *                   This file contains the common defines of the application.
  ******************************************************************************
  * @attention
  *
  * Copyright (c) 2022 STMicroelectronics.
  * All rights reserved.
  *
  * This software is licensed under terms that can be found in the LICENSE file
  * in the root directory of this software component.
  * If no LICENSE file comes with this software, it is provided AS-IS.
  *
  ******************************************************************************
  */
/* USER CODE END Header */

/* Define to prevent recursive inclusion -------------------------------------*/
#ifndef __MAIN_H
#define __MAIN_H

#ifdef __cplusplus
extern "C" {
#endif

/* Includes ------------------------------------------------------------------*/
#include "stm32f4xx_hal.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */

/* USER CODE END Includes */

/* Exported types ------------------------------------------------------------*/
/* USER CODE BEGIN ET */

/* USER CODE END ET */

/* Exported constants --------------------------------------------------------*/
/* USER CODE BEGIN EC */

/* USER CODE END EC */

/* Exported macro ------------------------------------------------------------*/
/* USER CODE BEGIN EM */

/* USER CODE END EM */

void HAL_TIM_MspPostInit(TIM_HandleTypeDef *htim);

/* Exported functions prototypes ---------------------------------------------*/
void Error_Handler(void);

/* USER CODE BEGIN EFP */

/* USER CODE END EFP */

/* Private defines -----------------------------------------------------------*/
#define OLED_SCL_Pin GPIO_PIN_5
#define OLED_SCL_GPIO_Port GPIOE
#define OLED_SDA_Pin GPIO_PIN_6
#define OLED_SDA_GPIO_Port GPIOE
#define IR_Pin GPIO_PIN_0
#define IR_GPIO_Port GPIOC
#define AIN2_Pin GPIO_PIN_2
#define AIN2_GPIO_Port GPIOA
#define AIN1_Pin GPIO_PIN_3
#define AIN1_GPIO_Port GPIOA
#define BIN1_Pin GPIO_PIN_4
#define BIN1_GPIO_Port GPIOA
#define BIN2_Pin GPIO_PIN_5
#define BIN2_GPIO_Port GPIOA
#define ENCODER_B1_Pin GPIO_PIN_6
#define ENCODER_B1_GPIO_Port GPIOA
#define ENCODER_B2_Pin GPIO_PIN_7
#define ENCODER_B2_GPIO_Port GPIOA
#define BATTERY_Pin GPIO_PIN_4
#define BATTERY_GPIO_Port GPIOC
#define OLED_RST_Pin GPIO_PIN_7
#define OLED_RST_GPIO_Port GPIOE
#define OLED_DC_Pin GPIO_PIN_8
#define OLED_DC_GPIO_Port GPIOE
#define LED_Pin GPIO_PIN_10
#define LED_GPIO_Port GPIOE
#define SERVO_Pin GPIO_PIN_14
#define SERVO_GPIO_Port GPIOE
#define SW1_Pin GPIO_PIN_8
#define SW1_GPIO_Port GPIOD
#define SW1_EXTI_IRQn EXTI9_5_IRQn
#define ULTRA_ECHO_Pin GPIO_PIN_14
#define ULTRA_ECHO_GPIO_Port GPIOD
#define PWMA_Pin GPIO_PIN_6
#define PWMA_GPIO_Port GPIOC
#define PWMB_Pin GPIO_PIN_7
#define PWMB_GPIO_Port GPIOC
#define ENCODER_A1_Pin GPIO_PIN_15
#define ENCODER_A1_GPIO_Port GPIOA
#define RPI_TX_Pin GPIO_PIN_10
#define RPI_TX_GPIO_Port GPIOC
#define RPI_RX_Pin GPIO_PIN_11
#define RPI_RX_GPIO_Port GPIOC
#define ENCODER_A2_Pin GPIO_PIN_3
#define ENCODER_A2_GPIO_Port GPIOB
#define ULTRA_TRIG_Pin GPIO_PIN_4
#define ULTRA_TRIG_GPIO_Port GPIOB
#define ICM_SCL_Pin GPIO_PIN_8
#define ICM_SCL_GPIO_Port GPIOB
#define ICM_SDA_Pin GPIO_PIN_9
#define ICM_SDA_GPIO_Port GPIOB
#define ENABLE_Pin GPIO_PIN_0
#define ENABLE_GPIO_Port GPIOE
/* USER CODE BEGIN Private defines */
extern I2C_HandleTypeDef hi2c1;
/* USER CODE END Private defines */

#ifdef __cplusplus
}
#endif

#endif /* __MAIN_H */
