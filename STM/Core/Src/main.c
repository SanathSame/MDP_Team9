/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : main.c
  * @brief          : Main program body
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
/* Includes ------------------------------------------------------------------*/
#include "main.h"
#include "cmsis_os.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "oled.h"
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */

/* USER CODE END PTD */

/* Private define ------------------------------------------------------------*/
/* USER CODE BEGIN PD */
/* USER CODE END PD */

/* Private macro -------------------------------------------------------------*/
/* USER CODE BEGIN PM */

/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/
ADC_HandleTypeDef hadc1;
ADC_HandleTypeDef hadc2;

TIM_HandleTypeDef htim1;
TIM_HandleTypeDef htim2;
TIM_HandleTypeDef htim3;
TIM_HandleTypeDef htim8;

UART_HandleTypeDef huart3;

/* Definitions for OLEDTask */
osThreadId_t OLEDTaskHandle;
const osThreadAttr_t OLEDTask_attributes = {
  .name = "OLEDTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};
/* Definitions for IRTask */
osThreadId_t IRTaskHandle;
const osThreadAttr_t IRTask_attributes = {
  .name = "IRTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};
/* Definitions for RPiTask */
osThreadId_t RPiTaskHandle;
const osThreadAttr_t RPiTask_attributes = {
  .name = "RPiTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};
/* Definitions for ServoTask */
osThreadId_t ServoTaskHandle;
const osThreadAttr_t ServoTask_attributes = {
  .name = "ServoTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};
/* Definitions for MotorATask */
osThreadId_t MotorATaskHandle;
const osThreadAttr_t MotorATask_attributes = {
  .name = "MotorATask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};
/* Definitions for MotorBTask */
osThreadId_t MotorBTaskHandle;
const osThreadAttr_t MotorBTask_attributes = {
  .name = "MotorBTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};
/* Definitions for EncoderATask */
osThreadId_t EncoderATaskHandle;
const osThreadAttr_t EncoderATask_attributes = {
  .name = "EncoderATask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};
/* Definitions for EncoderBTask */
osThreadId_t EncoderBTaskHandle;
const osThreadAttr_t EncoderBTask_attributes = {
  .name = "EncoderBTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};
/* Definitions for DispatchTask */
osThreadId_t DispatchTaskHandle;
const osThreadAttr_t DispatchTask_attributes = {
  .name = "DispatchTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};
/* Definitions for BatteryTask */
osThreadId_t BatteryTaskHandle;
const osThreadAttr_t BatteryTask_attributes = {
  .name = "BatteryTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};
/* Definitions for LEDTask */
osThreadId_t LEDTaskHandle;
const osThreadAttr_t LEDTask_attributes = {
  .name = "LEDTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};
/* USER CODE BEGIN PV */

/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
void SystemClock_Config(void);
static void MX_GPIO_Init(void);
static void MX_TIM1_Init(void);
static void MX_TIM2_Init(void);
static void MX_TIM3_Init(void);
static void MX_TIM8_Init(void);
static void MX_USART3_UART_Init(void);
static void MX_ADC1_Init(void);
static void MX_ADC2_Init(void);
void oled(void *argument);
void ir(void *argument);
void rpi(void *argument);
void servo(void *argument);
void motorA(void *argument);
void motorB(void *argument);
void encoderA(void *argument);
void encoderB(void *argument);
void dispatch(void *argument);
void battery(void *argument);
void led(void *argument);

/* USER CODE BEGIN PFP */
int irToDist();
void changeProfile();
void driveRobot(uint8_t *cmd);
void stopRobot();
void turnRobot(uint8_t *cmd);
/* USER CODE END PFP */

/* Private user code ---------------------------------------------------------*/
/* USER CODE BEGIN 0 */
float pi, radius;
float encoderGrad, encoderInt;
float kdb, kdf, kib, kif, kpb, kpf;
float distA, distB, distBuffer, distOffset, robotDist, tempA, tempB, turnOffset;
float lbGrad, lbInt, lfGrad, lfInt, rbGrad, rbInt, rfGrad, rfInt;

int actionCounter, fillCounter;
int driveMotorPWM, maxMotorPWM, motorAVal, motorBVal, turnMotorPWM;
int countRequired, encoderAVal, encoderBVal, encoderTarget, targetCount;
int errorA, errorB, prevErrorA, prevErrorB, sumErrorA, sumErrorB;
int robotAngle;

uint8_t cmds[1000][20], cmdState, prevCmd[20], rxBuffer[20], txBuffer[20];
uint8_t driveACmd, driveBCmd, startDriving, startPID;
uint8_t angleCmd, servoCmd;
uint8_t profile;

uint32_t servoCenter, servoLeft, servoRight, servoVal;
uint32_t batteryCounter, batteryVal[5], irCounter, irVal[5];
uint32_t batteryDelay, dispatchDelay, encoderDelay, irDelay, ledDelay, motorDelay, oledDelay, rpiDelay, servoDelay;  // rpiDelay <= encoderDelay
/* USER CODE END 0 */

/**
  * @brief  The application entry point.
  * @retval int
  */
int main(void)
{
  /* USER CODE BEGIN 1 */

  /* USER CODE END 1 */

  /* MCU Configuration--------------------------------------------------------*/

  /* Reset of all peripherals, Initializes the Flash interface and the Systick. */
  HAL_Init();

  /* USER CODE BEGIN Init */

  /* USER CODE END Init */

  /* Configure the system clock */
  SystemClock_Config();

  /* USER CODE BEGIN SysInit */

  /* USER CODE END SysInit */

  /* Initialize all configured peripherals */
  MX_GPIO_Init();
  MX_TIM1_Init();
  MX_TIM2_Init();
  MX_TIM3_Init();
  MX_TIM8_Init();
  MX_USART3_UART_Init();
  MX_ADC1_Init();
  MX_ADC2_Init();
  /* USER CODE BEGIN 2 */
  OLED_Init();

  pi = 3.1415926536;
  radius = 3.35;

  profile = 2;
  changeProfile();

  actionCounter = 0;
  fillCounter = -1;

  driveMotorPWM = 9000;
  maxMotorPWM = 9500;
  turnMotorPWM = 3500;

  countRequired = 15;

  for (int counter = 0; counter < 1000; ++counter)
	memset(cmds[counter], 0, 20);
  cmdState = 1;
  memset(rxBuffer, 0, 20);
  memset(txBuffer, 0, 20);

  driveACmd = 0;
  driveBCmd = 0;
  startDriving = 0;
  startPID = 0;

  angleCmd = 0;
  servoCmd = 0;

  servoCenter = 74;
  servoLeft = 50;
  servoRight = 109;

  batteryCounter = 0;
  irCounter = 0;
  for (int counter = 0; counter < 5; ++counter)
  {
	batteryVal[counter] = 0;
	irVal[counter] = 0;
  }

  batteryDelay = 2000;
  dispatchDelay = 1;
  encoderDelay = 25;
  irDelay = 200;
  ledDelay = 3000;
  motorDelay = 1;
  oledDelay = 10;
  rpiDelay = 10;
  servoDelay = 700;
  /* USER CODE END 2 */

  /* Init scheduler */
  osKernelInitialize();

  /* USER CODE BEGIN RTOS_MUTEX */
  /* add mutexes, ... */
  /* USER CODE END RTOS_MUTEX */

  /* USER CODE BEGIN RTOS_SEMAPHORES */
  /* add semaphores, ... */
  /* USER CODE END RTOS_SEMAPHORES */

  /* USER CODE BEGIN RTOS_TIMERS */
  /* start timers, add new ones, ... */
  /* USER CODE END RTOS_TIMERS */

  /* USER CODE BEGIN RTOS_QUEUES */
  /* add queues, ... */
  /* USER CODE END RTOS_QUEUES */

  /* Create the thread(s) */
  /* creation of OLEDTask */
  OLEDTaskHandle = osThreadNew(oled, NULL, &OLEDTask_attributes);

  /* creation of IRTask */
  IRTaskHandle = osThreadNew(ir, NULL, &IRTask_attributes);

  /* creation of RPiTask */
  RPiTaskHandle = osThreadNew(rpi, NULL, &RPiTask_attributes);

  /* creation of ServoTask */
  ServoTaskHandle = osThreadNew(servo, NULL, &ServoTask_attributes);

  /* creation of MotorATask */
  MotorATaskHandle = osThreadNew(motorA, NULL, &MotorATask_attributes);

  /* creation of MotorBTask */
  MotorBTaskHandle = osThreadNew(motorB, NULL, &MotorBTask_attributes);

  /* creation of EncoderATask */
  EncoderATaskHandle = osThreadNew(encoderA, NULL, &EncoderATask_attributes);

  /* creation of EncoderBTask */
  EncoderBTaskHandle = osThreadNew(encoderB, NULL, &EncoderBTask_attributes);

  /* creation of DispatchTask */
  DispatchTaskHandle = osThreadNew(dispatch, NULL, &DispatchTask_attributes);

  /* creation of BatteryTask */
  BatteryTaskHandle = osThreadNew(battery, NULL, &BatteryTask_attributes);

  /* creation of LEDTask */
  LEDTaskHandle = osThreadNew(led, NULL, &LEDTask_attributes);

  /* USER CODE BEGIN RTOS_THREADS */
  /* add threads, ... */
  /* USER CODE END RTOS_THREADS */

  /* USER CODE BEGIN RTOS_EVENTS */
  /* add events, ... */
  /* USER CODE END RTOS_EVENTS */

  /* Start scheduler */
  osKernelStart();

  /* We should never get here as control is now taken by the scheduler */
  /* Infinite loop */
  /* USER CODE BEGIN WHILE */
  while (1)
  {
    /* USER CODE END WHILE */

    /* USER CODE BEGIN 3 */
  }
  /* USER CODE END 3 */
}

/**
  * @brief System Clock Configuration
  * @retval None
  */
void SystemClock_Config(void)
{
  RCC_OscInitTypeDef RCC_OscInitStruct = {0};
  RCC_ClkInitTypeDef RCC_ClkInitStruct = {0};

  /** Configure the main internal regulator output voltage
  */
  __HAL_RCC_PWR_CLK_ENABLE();
  __HAL_PWR_VOLTAGESCALING_CONFIG(PWR_REGULATOR_VOLTAGE_SCALE1);
  /** Initializes the RCC Oscillators according to the specified parameters
  * in the RCC_OscInitTypeDef structure.
  */
  RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSI;
  RCC_OscInitStruct.HSIState = RCC_HSI_ON;
  RCC_OscInitStruct.HSICalibrationValue = RCC_HSICALIBRATION_DEFAULT;
  RCC_OscInitStruct.PLL.PLLState = RCC_PLL_NONE;
  if (HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK)
  {
    Error_Handler();
  }
  /** Initializes the CPU, AHB and APB buses clocks
  */
  RCC_ClkInitStruct.ClockType = RCC_CLOCKTYPE_HCLK|RCC_CLOCKTYPE_SYSCLK
                              |RCC_CLOCKTYPE_PCLK1|RCC_CLOCKTYPE_PCLK2;
  RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_HSI;
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV1;
  RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV1;

  if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_0) != HAL_OK)
  {
    Error_Handler();
  }
}

/**
  * @brief ADC1 Initialization Function
  * @param None
  * @retval None
  */
static void MX_ADC1_Init(void)
{

  /* USER CODE BEGIN ADC1_Init 0 */

  /* USER CODE END ADC1_Init 0 */

  ADC_ChannelConfTypeDef sConfig = {0};

  /* USER CODE BEGIN ADC1_Init 1 */

  /* USER CODE END ADC1_Init 1 */
  /** Configure the global features of the ADC (Clock, Resolution, Data Alignment and number of conversion)
  */
  hadc1.Instance = ADC1;
  hadc1.Init.ClockPrescaler = ADC_CLOCK_SYNC_PCLK_DIV2;
  hadc1.Init.Resolution = ADC_RESOLUTION_12B;
  hadc1.Init.ScanConvMode = DISABLE;
  hadc1.Init.ContinuousConvMode = DISABLE;
  hadc1.Init.DiscontinuousConvMode = DISABLE;
  hadc1.Init.ExternalTrigConvEdge = ADC_EXTERNALTRIGCONVEDGE_NONE;
  hadc1.Init.ExternalTrigConv = ADC_SOFTWARE_START;
  hadc1.Init.DataAlign = ADC_DATAALIGN_RIGHT;
  hadc1.Init.NbrOfConversion = 1;
  hadc1.Init.DMAContinuousRequests = DISABLE;
  hadc1.Init.EOCSelection = ADC_EOC_SINGLE_CONV;
  if (HAL_ADC_Init(&hadc1) != HAL_OK)
  {
    Error_Handler();
  }
  /** Configure for the selected ADC regular channel its corresponding rank in the sequencer and its sample time.
  */
  sConfig.Channel = ADC_CHANNEL_10;
  sConfig.Rank = 1;
  sConfig.SamplingTime = ADC_SAMPLETIME_3CYCLES;
  if (HAL_ADC_ConfigChannel(&hadc1, &sConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN ADC1_Init 2 */

  /* USER CODE END ADC1_Init 2 */

}

/**
  * @brief ADC2 Initialization Function
  * @param None
  * @retval None
  */
static void MX_ADC2_Init(void)
{

  /* USER CODE BEGIN ADC2_Init 0 */

  /* USER CODE END ADC2_Init 0 */

  ADC_ChannelConfTypeDef sConfig = {0};

  /* USER CODE BEGIN ADC2_Init 1 */

  /* USER CODE END ADC2_Init 1 */
  /** Configure the global features of the ADC (Clock, Resolution, Data Alignment and number of conversion)
  */
  hadc2.Instance = ADC2;
  hadc2.Init.ClockPrescaler = ADC_CLOCK_SYNC_PCLK_DIV2;
  hadc2.Init.Resolution = ADC_RESOLUTION_12B;
  hadc2.Init.ScanConvMode = DISABLE;
  hadc2.Init.ContinuousConvMode = DISABLE;
  hadc2.Init.DiscontinuousConvMode = DISABLE;
  hadc2.Init.ExternalTrigConvEdge = ADC_EXTERNALTRIGCONVEDGE_NONE;
  hadc2.Init.ExternalTrigConv = ADC_SOFTWARE_START;
  hadc2.Init.DataAlign = ADC_DATAALIGN_RIGHT;
  hadc2.Init.NbrOfConversion = 1;
  hadc2.Init.DMAContinuousRequests = DISABLE;
  hadc2.Init.EOCSelection = ADC_EOC_SINGLE_CONV;
  if (HAL_ADC_Init(&hadc2) != HAL_OK)
  {
    Error_Handler();
  }
  /** Configure for the selected ADC regular channel its corresponding rank in the sequencer and its sample time.
  */
  sConfig.Channel = ADC_CHANNEL_14;
  sConfig.Rank = 1;
  sConfig.SamplingTime = ADC_SAMPLETIME_3CYCLES;
  if (HAL_ADC_ConfigChannel(&hadc2, &sConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN ADC2_Init 2 */

  /* USER CODE END ADC2_Init 2 */

}

/**
  * @brief TIM1 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM1_Init(void)
{

  /* USER CODE BEGIN TIM1_Init 0 */

  /* USER CODE END TIM1_Init 0 */

  TIM_MasterConfigTypeDef sMasterConfig = {0};
  TIM_OC_InitTypeDef sConfigOC = {0};
  TIM_BreakDeadTimeConfigTypeDef sBreakDeadTimeConfig = {0};

  /* USER CODE BEGIN TIM1_Init 1 */

  /* USER CODE END TIM1_Init 1 */
  htim1.Instance = TIM1;
  htim1.Init.Prescaler = 320;
  htim1.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim1.Init.Period = 1000;
  htim1.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim1.Init.RepetitionCounter = 0;
  htim1.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_ENABLE;
  if (HAL_TIM_PWM_Init(&htim1) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim1, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sConfigOC.OCMode = TIM_OCMODE_PWM1;
  sConfigOC.Pulse = 0;
  sConfigOC.OCPolarity = TIM_OCPOLARITY_HIGH;
  sConfigOC.OCFastMode = TIM_OCFAST_DISABLE;
  sConfigOC.OCIdleState = TIM_OCIDLESTATE_RESET;
  sConfigOC.OCNIdleState = TIM_OCNIDLESTATE_RESET;
  if (HAL_TIM_PWM_ConfigChannel(&htim1, &sConfigOC, TIM_CHANNEL_4) != HAL_OK)
  {
    Error_Handler();
  }
  sBreakDeadTimeConfig.OffStateRunMode = TIM_OSSR_DISABLE;
  sBreakDeadTimeConfig.OffStateIDLEMode = TIM_OSSI_DISABLE;
  sBreakDeadTimeConfig.LockLevel = TIM_LOCKLEVEL_OFF;
  sBreakDeadTimeConfig.DeadTime = 0;
  sBreakDeadTimeConfig.BreakState = TIM_BREAK_DISABLE;
  sBreakDeadTimeConfig.BreakPolarity = TIM_BREAKPOLARITY_HIGH;
  sBreakDeadTimeConfig.AutomaticOutput = TIM_AUTOMATICOUTPUT_DISABLE;
  if (HAL_TIMEx_ConfigBreakDeadTime(&htim1, &sBreakDeadTimeConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM1_Init 2 */

  /* USER CODE END TIM1_Init 2 */
  HAL_TIM_MspPostInit(&htim1);

}

/**
  * @brief TIM2 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM2_Init(void)
{

  /* USER CODE BEGIN TIM2_Init 0 */

  /* USER CODE END TIM2_Init 0 */

  TIM_Encoder_InitTypeDef sConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};

  /* USER CODE BEGIN TIM2_Init 1 */

  /* USER CODE END TIM2_Init 1 */
  htim2.Instance = TIM2;
  htim2.Init.Prescaler = 0;
  htim2.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim2.Init.Period = 65535;
  htim2.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim2.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  sConfig.EncoderMode = TIM_ENCODERMODE_TI12;
  sConfig.IC1Polarity = TIM_ICPOLARITY_RISING;
  sConfig.IC1Selection = TIM_ICSELECTION_DIRECTTI;
  sConfig.IC1Prescaler = TIM_ICPSC_DIV1;
  sConfig.IC1Filter = 10;
  sConfig.IC2Polarity = TIM_ICPOLARITY_RISING;
  sConfig.IC2Selection = TIM_ICSELECTION_DIRECTTI;
  sConfig.IC2Prescaler = TIM_ICPSC_DIV1;
  sConfig.IC2Filter = 10;
  if (HAL_TIM_Encoder_Init(&htim2, &sConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim2, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM2_Init 2 */

  /* USER CODE END TIM2_Init 2 */

}

/**
  * @brief TIM3 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM3_Init(void)
{

  /* USER CODE BEGIN TIM3_Init 0 */

  /* USER CODE END TIM3_Init 0 */

  TIM_Encoder_InitTypeDef sConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};

  /* USER CODE BEGIN TIM3_Init 1 */

  /* USER CODE END TIM3_Init 1 */
  htim3.Instance = TIM3;
  htim3.Init.Prescaler = 0;
  htim3.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim3.Init.Period = 65535;
  htim3.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim3.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  sConfig.EncoderMode = TIM_ENCODERMODE_TI12;
  sConfig.IC1Polarity = TIM_ICPOLARITY_RISING;
  sConfig.IC1Selection = TIM_ICSELECTION_DIRECTTI;
  sConfig.IC1Prescaler = TIM_ICPSC_DIV1;
  sConfig.IC1Filter = 10;
  sConfig.IC2Polarity = TIM_ICPOLARITY_RISING;
  sConfig.IC2Selection = TIM_ICSELECTION_DIRECTTI;
  sConfig.IC2Prescaler = TIM_ICPSC_DIV1;
  sConfig.IC2Filter = 10;
  if (HAL_TIM_Encoder_Init(&htim3, &sConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim3, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM3_Init 2 */

  /* USER CODE END TIM3_Init 2 */

}

/**
  * @brief TIM8 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM8_Init(void)
{

  /* USER CODE BEGIN TIM8_Init 0 */

  /* USER CODE END TIM8_Init 0 */

  TIM_ClockConfigTypeDef sClockSourceConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};
  TIM_OC_InitTypeDef sConfigOC = {0};
  TIM_BreakDeadTimeConfigTypeDef sBreakDeadTimeConfig = {0};

  /* USER CODE BEGIN TIM8_Init 1 */

  /* USER CODE END TIM8_Init 1 */
  htim8.Instance = TIM8;
  htim8.Init.Prescaler = 0;
  htim8.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim8.Init.Period = 16799;
  htim8.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim8.Init.RepetitionCounter = 0;
  htim8.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  if (HAL_TIM_Base_Init(&htim8) != HAL_OK)
  {
    Error_Handler();
  }
  sClockSourceConfig.ClockSource = TIM_CLOCKSOURCE_INTERNAL;
  if (HAL_TIM_ConfigClockSource(&htim8, &sClockSourceConfig) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_PWM_Init(&htim8) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim8, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sConfigOC.OCMode = TIM_OCMODE_PWM1;
  sConfigOC.Pulse = 0;
  sConfigOC.OCPolarity = TIM_OCPOLARITY_HIGH;
  sConfigOC.OCNPolarity = TIM_OCNPOLARITY_HIGH;
  sConfigOC.OCFastMode = TIM_OCFAST_DISABLE;
  sConfigOC.OCIdleState = TIM_OCIDLESTATE_RESET;
  sConfigOC.OCNIdleState = TIM_OCNIDLESTATE_RESET;
  if (HAL_TIM_PWM_ConfigChannel(&htim8, &sConfigOC, TIM_CHANNEL_1) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_PWM_ConfigChannel(&htim8, &sConfigOC, TIM_CHANNEL_2) != HAL_OK)
  {
    Error_Handler();
  }
  sBreakDeadTimeConfig.OffStateRunMode = TIM_OSSR_DISABLE;
  sBreakDeadTimeConfig.OffStateIDLEMode = TIM_OSSI_DISABLE;
  sBreakDeadTimeConfig.LockLevel = TIM_LOCKLEVEL_OFF;
  sBreakDeadTimeConfig.DeadTime = 0;
  sBreakDeadTimeConfig.BreakState = TIM_BREAK_DISABLE;
  sBreakDeadTimeConfig.BreakPolarity = TIM_BREAKPOLARITY_HIGH;
  sBreakDeadTimeConfig.AutomaticOutput = TIM_AUTOMATICOUTPUT_DISABLE;
  if (HAL_TIMEx_ConfigBreakDeadTime(&htim8, &sBreakDeadTimeConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM8_Init 2 */

  /* USER CODE END TIM8_Init 2 */

}

/**
  * @brief USART3 Initialization Function
  * @param None
  * @retval None
  */
static void MX_USART3_UART_Init(void)
{

  /* USER CODE BEGIN USART3_Init 0 */

  /* USER CODE END USART3_Init 0 */

  /* USER CODE BEGIN USART3_Init 1 */

  /* USER CODE END USART3_Init 1 */
  huart3.Instance = USART3;
  huart3.Init.BaudRate = 115200;
  huart3.Init.WordLength = UART_WORDLENGTH_8B;
  huart3.Init.StopBits = UART_STOPBITS_1;
  huart3.Init.Parity = UART_PARITY_NONE;
  huart3.Init.Mode = UART_MODE_TX_RX;
  huart3.Init.HwFlowCtl = UART_HWCONTROL_NONE;
  huart3.Init.OverSampling = UART_OVERSAMPLING_16;
  if (HAL_UART_Init(&huart3) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN USART3_Init 2 */

  /* USER CODE END USART3_Init 2 */

}

/**
  * @brief GPIO Initialization Function
  * @param None
  * @retval None
  */
static void MX_GPIO_Init(void)
{
  GPIO_InitTypeDef GPIO_InitStruct = {0};

  /* GPIO Ports Clock Enable */
  __HAL_RCC_GPIOE_CLK_ENABLE();
  __HAL_RCC_GPIOC_CLK_ENABLE();
  __HAL_RCC_GPIOA_CLK_ENABLE();
  __HAL_RCC_GPIOD_CLK_ENABLE();
  __HAL_RCC_GPIOB_CLK_ENABLE();

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOE, OLED_SCL_Pin|OLED_SDA_Pin|OLED_RST_Pin|OLED_DC_Pin
                          |LED_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOA, AIN2_Pin|AIN1_Pin|BIN1_Pin|BIN2_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pins : OLED_SCL_Pin OLED_SDA_Pin OLED_RST_Pin OLED_DC_Pin
                           LED_Pin */
  GPIO_InitStruct.Pin = OLED_SCL_Pin|OLED_SDA_Pin|OLED_RST_Pin|OLED_DC_Pin
                          |LED_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(GPIOE, &GPIO_InitStruct);

  /*Configure GPIO pins : AIN2_Pin AIN1_Pin BIN1_Pin BIN2_Pin */
  GPIO_InitStruct.Pin = AIN2_Pin|AIN1_Pin|BIN1_Pin|BIN2_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_HIGH;
  HAL_GPIO_Init(GPIOA, &GPIO_InitStruct);

  /*Configure GPIO pin : SW1_Pin */
  GPIO_InitStruct.Pin = SW1_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_IT_FALLING;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  HAL_GPIO_Init(SW1_GPIO_Port, &GPIO_InitStruct);

  /* EXTI interrupt init*/
  HAL_NVIC_SetPriority(EXTI9_5_IRQn, 5, 0);
  HAL_NVIC_EnableIRQ(EXTI9_5_IRQn);

}

/* USER CODE BEGIN 4 */
void HAL_GPIO_EXTI_Callback(uint16_t GPIO_Pin)
{
  if (GPIO_Pin == SW1_Pin)
  {
	profile = (profile + 1) % 3;
	changeProfile();
  }
}

void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart)
{
  UNUSED(huart);
}

void celibate(uint8_t *cmd)
{
	char distStr[5];
	strncpy(distStr, (char*)(cmd + 6), 4);
	int delay = atoi(distStr);

	startDriving = 1;
	tempA = 0;
	tempB = 0;

	motorAVal = driveMotorPWM;
	motorBVal = motorAVal;

	driveACmd = cmd[4];
	driveBCmd = cmd[4];

	osDelay(delay);
	stopRobot();
}

int irToDist()
{
  float avgIRVal = 0.0;

  for (int counter = 0; counter < 5; ++counter)
  	avgIRVal += irVal[counter];

  avgIRVal /= 5;

  return (int)(avgIRVal + 0.5);
}

uint8_t rxFilled(uint8_t size)
{
  for (int counter = 0; counter < size; ++counter)
	if (rxBuffer[counter] == 0)
	  return 0;

  return 1;
}

void changeProfile()
{
  switch (profile)
  {
  case 0: //TR
    encoderGrad = 0.208211985127613;
	encoderInt = 0.556356667437655;

	kdb = 2;
	kdf = 1.5;
	kib = 1.5;
	kif = 2;
	kpb = 8;
	kpf = 8.5;

	distOffset = 5;
	turnOffset = 0.5;

	lbGrad = 0.406223321414319;
	lbInt = -1.03827694476358;
	lfGrad = 0.374241777484367;
	lfInt = -0.80255211795561;
	rbGrad = 0.390119353921512;
	rbInt = 1.405188423049935;
	rfGrad = 0.364677963587823;
	rfInt = 1.011921094590065;

	break;

  case 1: //Lab
	encoderGrad = 0.20959044667335;
	encoderInt = 1.10778227509323;

	kdb = 0;
	kdf = 0;
	kib = 0;
	kif = 0;
	kpb = 0;
	kpf = 0;

	distOffset = 5;
	turnOffset = 0;

	lbGrad = 1;
	lbInt = 0;
	lfGrad = 1;
	lfInt = 0;
	rbGrad = 1;
	rbInt = 0;
	rfGrad = 1;
	rfInt = 0;

	break;

  case 2: //Outside Lab
	encoderGrad = 0.208496572267945;
	encoderInt = 1.08776618891631;

	kdb = 0;
	kdf = 0;
	kib = 0;
	kif = 0;
	kpb = 0;
	kpf = 0;

	distOffset = 4;
	turnOffset = 1;

	lbGrad = 0.477744248433485;
	lbInt = 2.02776765331444;
	lfGrad = 0.425142145382334;
	lfInt = 1.59811982955917;
	rbGrad = 0.452958458665014;
	rbInt = 3.65536165354082;
	rfGrad = 0.381832587414008;
	rfInt = 2.05906630142695;

	break;
  }
}

void driveRobot(uint8_t *cmd)
{
  char distStr[5];
  strncpy(distStr, (char*)(cmd + 6), 4);
  if (angleCmd == 0)
	robotDist = (float)atoi(distStr);

  driveACmd = cmd[4];
  driveBCmd = cmd[4];

  distBuffer = angleCmd == 0 ? distOffset : turnOffset;

  if (startDriving == 0)
  {
	startDriving = 1;
	startPID = angleCmd == 0 ? 1 : 0;

	targetCount = 0;
	encoderTarget = 0;

	prevErrorA = 0;
	prevErrorB = 0;
	sumErrorA = 0;
	sumErrorB = 0;

	distA = 0.0;
	distB = 0.0;
	tempA = 0.0;
	tempB = 0.0;

	motorAVal = angleCmd == 0 ? driveMotorPWM : turnMotorPWM;
	motorBVal = motorAVal;
  }
  else if (distA >= robotDist - distBuffer && distB >= robotDist - distBuffer)
	stopRobot();
}

void stopRobot()
{
  startPID = 0;

  motorAVal = 0;
  motorBVal = 0;
  driveACmd = 0;
  driveBCmd = 0;
  osDelay(15 * motorDelay);

  servoVal = servoCenter;
  servoCmd = 0;
  osDelay((uint32_t)(1.2 * servoDelay));

  startDriving = 0;
  if (angleCmd != 0)
  {
    strncpy((char*)cmds[actionCounter], (char*)prevCmd, 20);
    angleCmd = 0;
  }

  cmdState = 2;
}

void turnRobot(uint8_t *cmd)
{
  char angleStr[4];
  strncpy(angleStr, (char*)(cmd + 7), 3);
  robotAngle = atoi(angleStr);

  angleCmd = cmd[4];
  driveACmd = cmd[5];
  driveBCmd = cmd[5];

  if (angleCmd == 'L' && driveACmd == 'B')
  	robotDist = lbGrad * robotAngle + lbInt;
  else if (angleCmd == 'L' && driveACmd == 'F')
	robotDist = lfGrad * robotAngle + lfInt;
  else if (angleCmd == 'R' && driveACmd == 'B')
	robotDist = rbGrad * robotAngle + rbInt;
  else if (angleCmd == 'R' && driveACmd == 'F')
	robotDist = rfGrad * robotAngle + rfInt;

  if (robotDist > 0.0)
  {
    servoVal = angleCmd == 'L' ? servoLeft : servoRight;
    servoCmd = 'T';

    osDelay((uint32_t)(1.2 * servoDelay));

    strncpy((char*)prevCmd, (char*)cmd, 20);
    strncpy((char*)cmds[actionCounter], (char*)cmd, 4);
    sprintf((char*)(cmds[actionCounter] + 4), "%c %4d", (char)driveACmd, (int)(robotDist + 0.5));
  }
}
/* USER CODE END 4 */

/* USER CODE BEGIN Header_oled */
/**
* @brief Function implementing the OLEDTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_oled */
void oled(void *argument)
{
  /* USER CODE BEGIN 5 */
  char strBuffer[20];
  float avgBatteryVal;
  /* Infinite loop */
  for(;;)
  {
	sprintf(strBuffer, "%d", profile);
	OLED_ShowString(0, 0, (uint8_t*)strBuffer);

	sprintf(strBuffer, "          ");
	if (actionCounter <= fillCounter && cmds[actionCounter][0] != 0)
      strncpy(strBuffer, (char*)(angleCmd != 0 ? prevCmd : cmds[actionCounter]), 20);
	OLED_ShowString(10, 0, (uint8_t*)strBuffer);

	//sprintf(strBuffer, "DistA: %-5d", (int)(distA + 0.5));
    //sprintf(strBuffer, "MotorA: %-5d", (int)motorAVal);
	//sprintf(strBuffer, "EncA: %-5d", (int)encoderAVal);
	sprintf(strBuffer, "IR: %-5d", irToDist());
	//sprintf(strBuffer, "Ultra: %-5d", (int)(ultraDist + 0.5));
	//sprintf(strBuffer, "Tick1: %-5d", (int)tick1);
	//sprintf(strBuffer, "TempA: %-5d", (int)tempA);
	OLED_ShowString(10, 10, (uint8_t*)strBuffer);

	//sprintf(strBuffer, "DistB: %-5d", (int)(distB + 0.5));
	//sprintf(strBuffer, "MotorB: %-5d", (int)motorBVal);
	//sprintf(strBuffer, "EncB: %-5d", (int)encoderBVal);
	//sprintf(strBuffer, "Tick2: %-5d", (int)tick2);
	//sprintf(strBuffer, "TempB: %-5d", (int)tempB);
	//OLED_ShowString(10, 20, (uint8_t*)strBuffer);

	//sprintf(strBuffer, "Action: %-5d", (int)actionCounter);
	sprintf(strBuffer, "DistA: %-5d", (int)(distA + 0.5));
	//sprintf(strBuffer, "TempA: %-5d", (int)tempA);
	OLED_ShowString(10, 30, (uint8_t*)strBuffer);

	//sprintf(strBuffer, "Fill: %-5d", (int)fillCounter);
	sprintf(strBuffer, "DistB: %-5d", (int)(distB + 0.5));
	//sprintf(strBuffer, "TempB: %-5d", (int)tempB);
	OLED_ShowString(10, 40, (uint8_t*)strBuffer);

	avgBatteryVal = 0.0;
	for (int counter = 0; counter < 5; ++counter)
	  avgBatteryVal += batteryVal[counter];

	avgBatteryVal /= 5;

	sprintf(strBuffer, "Battery: %d", (int)(avgBatteryVal + 0.5));
	OLED_ShowString(10, 50, (uint8_t*)strBuffer);

	OLED_Refresh_Gram();
    osDelay(oledDelay);
  }
  /* USER CODE END 5 */
}

/* USER CODE BEGIN Header_ir */
/**
* @brief Function implementing the IRTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_ir */
void ir(void *argument)
{
  /* USER CODE BEGIN ir */
  //int dist;
  /* Infinite loop */
  for(;;)
  {
	if (irCounter > 4)
	  for (irCounter = 0; irCounter < 4; ++irCounter)
	    irVal[irCounter] = irVal[irCounter + 1];

	HAL_ADC_Start(&hadc1);
	HAL_ADC_PollForConversion(&hadc1, 0xFFFF);
	irVal[irCounter++] = HAL_ADC_GetValue(&hadc1);

    osDelay(irDelay);
  }
  /* USER CODE END ir */
}

/* USER CODE BEGIN Header_rpi */
/**
* @brief Function implementing the RPiTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_rpi */
void rpi(void *argument)
{
  /* USER CODE BEGIN rpi */
  char strCounter[4];
  int cmdCounter;
  uint8_t cmdType;
  /* Infinite loop */
  for(;;)
  {
	HAL_UART_Receive_IT(&huart3, rxBuffer, 10);

	if (rxFilled(10) == 1)
	{
	  strncpy(strCounter, (char*)rxBuffer, 3);
      cmdCounter = atoi(strCounter);

      if (cmdCounter >= 0 && cmdCounter < 1000)
      {
        strncpy((char*)cmds[cmdCounter], (char*)rxBuffer, 20);

        if (cmdCounter > fillCounter)
          fillCounter = cmdCounter;
      }

      memset(rxBuffer, 0, 20);
	}

    if (cmdState == 2)
    {
      cmdState = 0;
      cmdType = cmds[actionCounter][4];

      strncpy(strCounter, (char*)cmds[actionCounter], 3);
      //sprintf((char*)txBuffer, "%d %d", (int)tempA, (int)tempB);
      sprintf((char*)txBuffer, cmdType == 'C' ? "Done C" : "Done %d", atoi(strCounter));
      HAL_UART_Transmit(&huart3, txBuffer, 20, 0xFFFF);

      if (cmdType == 'C')
      {
    	actionCounter = 0;
    	fillCounter = -1;
    	cmdState = 1;

    	for (int counter = 0; counter < 1000; ++counter)
    	  memset(cmds[counter], 0, 20);
      }
    }

    if (cmdState == 0 && actionCounter <= fillCounter)
    {
      cmdState = 1;
      ++actionCounter;
    }

    osDelay(rpiDelay);
  }
  /* USER CODE END rpi */
}

/* USER CODE BEGIN Header_servo */
/**
* @brief Function implementing the ServoTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_servo */
void servo(void *argument)
{
  /* USER CODE BEGIN servo */
  HAL_TIM_PWM_Start(&htim1, TIM_CHANNEL_4);
  /* Infinite loop */
  for(;;)
  {
	if (servoVal < servoLeft)
	  servoVal = servoLeft;
	else if (servoVal > servoRight)
	  servoVal = servoRight;

	if (servoCmd == 'T')
	  htim1.Instance->CCR4 = servoVal;
	else
	  htim1.Instance->CCR4 = servoCenter;

	osDelay(servoDelay);
  }
  /* USER CODE END servo */
}

/* USER CODE BEGIN Header_motorA */
/**
* @brief Function implementing the MotorATask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_motorA */
void motorA(void *argument)
{
  /* USER CODE BEGIN motorA */
  HAL_TIM_PWM_Start_IT(&htim8, TIM_CHANNEL_1);
  /* Infinite loop */
  for(;;)
  {
	if (motorAVal < 0)
	  motorAVal = 0;
	else if (motorAVal > maxMotorPWM)
	  motorAVal = maxMotorPWM;

	if (driveACmd == 'F')
	{
	  HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_SET);
	  HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_RESET);
	  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, (uint32_t)motorAVal);
	}
	else if (driveACmd == 'B')
	{
	  HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_RESET);
	  HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_SET);
	  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, (uint32_t)motorAVal);
	}
	else
	  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, 0);

    osDelay(motorDelay);
  }
  /* USER CODE END motorA */
}

/* USER CODE BEGIN Header_motorB */
/**
* @brief Function implementing the motorBTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_motorB */
void motorB(void *argument)
{
  /* USER CODE BEGIN motorB */
  HAL_TIM_PWM_Start_IT(&htim8, TIM_CHANNEL_2);
  /* Infinite loop */
  for(;;)
  {
	if (motorBVal < 0)
	  motorBVal = 0;
	else if (motorBVal > maxMotorPWM)
	  motorBVal = maxMotorPWM;

	if (driveBCmd == 'F')
	{
	  HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_SET);
	  HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_RESET);
	  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, (uint32_t)motorBVal);
	}
	else if (driveBCmd == 'B')
	{
	  HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_RESET);
	  HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_SET);
	  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, (uint32_t)motorBVal);
	}
	else
	  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, 0);

    osDelay(motorDelay);
  }
  /* USER CODE END motorB */
}

/* USER CODE BEGIN Header_encoderA */
/**
* @brief Function implementing the EncoderATask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_encoderA */
void encoderA(void *argument)
{
  /* USER CODE BEGIN encoderA */
  uint32_t cnt1, cnt2, diff1, diff2, tick;

  HAL_TIM_Encoder_Start(&htim2, TIM_CHANNEL_ALL);
  cnt1 = __HAL_TIM_GET_COUNTER(&htim2);
  tick = HAL_GetTick();
  /* Infinite loop */
  for(;;)
  {
	if (HAL_GetTick() - tick > encoderDelay)
	{
	  cnt2 = __HAL_TIM_GET_COUNTER(&htim2);

	  diff1 = abs(cnt1 - cnt2);
	  diff2 = abs(65535 - (__HAL_TIM_IS_TIM_COUNTING_DOWN(&htim2) ? cnt2 - cnt1 : cnt1 - cnt2));

	  encoderAVal = diff1 < diff2 ? diff1 : diff2;

	  if (startDriving == 1)
	  {
		tempA += pi * radius * encoderAVal / 165;

		if (distA == 0.0)
		  distA += encoderInt;

		distA += encoderGrad * pi * radius * encoderAVal / 165;
	  }

	  if (startPID == 1)
	  {
		if (encoderTarget == 0 && targetCount++ == countRequired)
		  encoderTarget = encoderAVal <= encoderBVal ? encoderAVal : encoderBVal;
		else if (angleCmd == 0 && encoderTarget != 0)
		{
		  errorA = encoderTarget - encoderAVal;
		  motorAVal += (int)(driveACmd == 'F' ? kpf * errorA + kdf * prevErrorA + kif * sumErrorA : kpb * errorA + kdb * prevErrorA + kib * sumErrorA);
		  prevErrorA = errorA;
		  sumErrorA += errorA;
		}
	  }

	  cnt1 = __HAL_TIM_GET_COUNTER(&htim2);
	  tick = HAL_GetTick();
	}
  }
  /* USER CODE END encoderA */
}

/* USER CODE BEGIN Header_encoderB */
/**
* @brief Function implementing the EncoderBTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_encoderB */
void encoderB(void *argument)
{
  /* USER CODE BEGIN encoderB */
  uint32_t cnt1, cnt2, diff1, diff2, tick;

  HAL_TIM_Encoder_Start(&htim3, TIM_CHANNEL_ALL);
  cnt1 = __HAL_TIM_GET_COUNTER(&htim3);
  tick = HAL_GetTick();
  /* Infinite loop */
  for(;;)
  {
	if (HAL_GetTick() - tick > encoderDelay)
	{
	  cnt2 = __HAL_TIM_GET_COUNTER(&htim3);

	  diff1 = abs(cnt1 - cnt2);
	  diff2 = abs(65535 - (__HAL_TIM_IS_TIM_COUNTING_DOWN(&htim3) ? cnt2 - cnt1 : cnt1 - cnt2));

	  encoderBVal = diff1 < diff2 ? diff1 : diff2;

	  if (startDriving == 1)
	  {
		tempB += pi * radius * encoderBVal / 165;

		if (distB == 0.0)
		  distB += encoderInt;

		distB += encoderGrad * pi * radius * encoderBVal / 165;
	  }

	  if (startPID == 1)
	  {
		if (encoderTarget == 0 && targetCount++ == countRequired)
		  encoderTarget = encoderAVal <= encoderBVal ? encoderAVal : encoderBVal;
		else if (angleCmd == 0 && encoderTarget != 0)
	    {
	      errorB = encoderTarget - encoderBVal;
	  	  motorBVal += (int)(int)(driveACmd == 'F' ? kpf * errorB + kdf * prevErrorB + kif * sumErrorB : kpb * errorB + kdb * prevErrorB + kib * sumErrorB);
	  	  prevErrorB = errorB;
	  	  sumErrorB += errorB;
	    }
	  }

	  cnt1 = __HAL_TIM_GET_COUNTER(&htim3);
	  tick = HAL_GetTick();
	}
  }
  /* USER CODE END encoderB */
}

/* USER CODE BEGIN Header_dispatch */
/**
* @brief Function implementing the DispatchTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_dispatch */
void dispatch(void *argument)
{
  /* USER CODE BEGIN dispatch */
  char strCounter[5];
  int cmdCounter;
  uint8_t cmdType;
  /* Infinite loop */
  for(;;)
  {
	if (cmdState == 1 && actionCounter <= fillCounter)
	{
	  cmdType = cmds[actionCounter][4];

	  if (cmdType == 'F' || cmdType == 'B')
		//celibate(cmds[actionCounter]);
	    driveRobot(cmds[actionCounter]);
	  else if (cmdType == 'L' || cmdType == 'R')
	    turnRobot(cmds[actionCounter]);
	  else if (cmdType == 'S')
	    stopRobot();
	  else if (cmdType == 'J')
	  {
		strncpy(strCounter, (char*)(cmds[actionCounter] + 6), 4);
		cmdCounter = atoi(strCounter);

		if (cmdCounter >= 0 && cmdCounter < 1000 && cmdCounter <= fillCounter)
		  actionCounter = cmdCounter;
	  }
	  else if (cmdType == 'C')
		cmdState = 2;
	  else
		++actionCounter;
	}

	osDelay(dispatchDelay);
  }
  /* USER CODE END dispatch */
}

/* USER CODE BEGIN Header_battery */
/**
* @brief Function implementing the BatteryTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_battery */
void battery(void *argument)
{
  /* USER CODE BEGIN battery */
  /* Infinite loop */
  for(;;)
  {
	if (batteryCounter > 4)
	  for (batteryCounter = 0; batteryCounter < 4; ++batteryCounter)
		batteryVal[batteryCounter] = batteryVal[batteryCounter + 1];

	HAL_ADC_Start(&hadc2);
	HAL_ADC_PollForConversion(&hadc2, 0xFFFF);
	batteryVal[batteryCounter++] = HAL_ADC_GetValue(&hadc2);

	osDelay(batteryDelay);
  }
  /* USER CODE END battery */
}

/* USER CODE BEGIN Header_led */
/**
* @brief Function implementing the LEDTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_led */
void led(void *argument)
{
  /* USER CODE BEGIN led */
  /* Infinite loop */
  for(;;)
  {
	HAL_GPIO_TogglePin(GPIOE, LED_Pin);
	osDelay(ledDelay);
  }
  /* USER CODE END led */
}

/**
  * @brief  This function is executed in case of error occurrence.
  * @retval None
  */
void Error_Handler(void)
{
  /* USER CODE BEGIN Error_Handler_Debug */
  /* User can add his own implementation to report the HAL error return state */
  __disable_irq();
  while (1)
  {
  }
  /* USER CODE END Error_Handler_Debug */
}

#ifdef  USE_FULL_ASSERT
/**
  * @brief  Reports the name of the source file and the source line number
  *         where the assert_param error has occurred.
  * @param  file: pointer to the source file name
  * @param  line: assert_param error line source number
  * @retval None
  */
void assert_failed(uint8_t *file, uint32_t line)
{
  /* USER CODE BEGIN 6 */
  /* User can add his own implementation to report the file name and line number,
     ex: printf("Wrong parameters value: file %s on line %d\r\n", file, line) */
  /* USER CODE END 6 */
}
#endif /* USE_FULL_ASSERT */

