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
TIM_HandleTypeDef htim4;
TIM_HandleTypeDef htim5;
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
/* Definitions for UltraTask */
osThreadId_t UltraTaskHandle;
const osThreadAttr_t UltraTask_attributes = {
  .name = "UltraTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};
/* Definitions for MotorTask */
osThreadId_t MotorTaskHandle;
const osThreadAttr_t MotorTask_attributes = {
  .name = "MotorTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};
/* Definitions for EncoderTask */
osThreadId_t EncoderTaskHandle;
const osThreadAttr_t EncoderTask_attributes = {
  .name = "EncoderTask",
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
static void MX_TIM5_Init(void);
static void MX_TIM4_Init(void);
void oled(void *argument);
void ir(void *argument);
void rpi(void *argument);
void servo(void *argument);
void dispatch(void *argument);
void battery(void *argument);
void led(void *argument);
void ultra(void *argument);
void motor(void *argument);
void encoder(void *argument);

/* USER CODE BEGIN PFP */
float avgVal(uint32_t *val, uint32_t size, float grad, float inter);
uint8_t bufferFilled(uint8_t *buffer, uint32_t size);
void microDelay(uint32_t us);
void valShift(uint32_t *val, uint32_t size);

void changeProfile();
void driveRobot(uint8_t *cmd);
void pid(uint32_t dur);
void stopRobot();
void turnRobot(uint8_t *cmd);
/* USER CODE END PFP */

/* Private user code ---------------------------------------------------------*/
/* USER CODE BEGIN 0 */
float pi, radius;
float encoderGrad, encoderInt;
float kdb, kdf, kib, kif, kpb, kpf;
float iTermA, iTermB;
float distA, distB, distBuffer, distOffset, robotDist, tempA, tempB, turnOffset;
float lbGrad, lbInt, lfGrad, lfInt, rbGrad, rbInt, rfGrad, rfInt;
float irGrad, irInt, ultraGrad, ultraInt;

int actionCounter, fillCounter;
int driveMotorPWM, maxMotorPWM, motorAVal, motorBVal, turnMotorPWM;
int encoderAVal, encoderBVal, encoderTarget;
int errorA, errorB, prevEncoderA, prevEncoderB;
int robotAngle;

uint8_t cmds[1000][20], cmdState, rxBuffer[20], txBuffer[20];
uint8_t angleCmd, driveCmd, enablePID, startDriving, startPID;
uint8_t batteryState[11];
uint8_t ultraCapture;
uint8_t profile;

uint32_t servoCenter, servoLeft, servoRight, servoVal;
uint32_t batteryCounter, batteryVal[5], irCounter, irVal[5], ultraCounter, ultraVal[5];
uint32_t batteryDelay, dispatchDelay, encoderDelay, irDelay, ledDelay, motorDelay, oledDelay, rpiDelay, servoDelay, ultraDelay;  // rpiDelay <= encoderDelay
uint32_t stoppingDelay, turningDelay;
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
  MX_TIM5_Init();
  MX_TIM4_Init();
  /* USER CODE BEGIN 2 */
  OLED_Init();

  pi = 3.1415926536;
  radius = 3.35;

  profile = 2;
  changeProfile();

  irGrad = 1;
  irInt = 0;
  ultraGrad = 0.01715;
  ultraInt = 0;

  actionCounter = 0;
  fillCounter = -1;

  driveMotorPWM = 9000;
  maxMotorPWM = 10000;
  turnMotorPWM = 3500;

  for (int counter = 0; counter < 1000; ++counter)
	memset(cmds[counter], 0, 20);

  cmdState = 1;

  memset(rxBuffer, 0, 20);
  memset(txBuffer, 0, 20);

  angleCmd = 0;
  driveCmd = 0;
  enablePID = 0;
  startDriving = 0;
  startPID = 0;

  sprintf((char*)batteryState, "Batt:     ");

  ultraCapture = 0;

  servoCenter = 74;
  servoLeft = 50;
  servoRight = 109;

  batteryCounter = 0;
  irCounter = 0;
  ultraCounter = 0;
  for (int counter = 0; counter < 5; ++counter)
  {
	batteryVal[counter] = 0;
	irVal[counter] = 0;
	ultraVal[counter] = 0;
  }

  batteryDelay = 2000;
  dispatchDelay = 1;
  encoderDelay = 25;
  irDelay = 100;
  ledDelay = 3000;
  motorDelay = 1;
  oledDelay = 10;
  rpiDelay = 1;
  servoDelay = 1;
  ultraDelay = 50;

  stoppingDelay = 100;
  turningDelay = 1000;
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

  /* creation of DispatchTask */
  DispatchTaskHandle = osThreadNew(dispatch, NULL, &DispatchTask_attributes);

  /* creation of BatteryTask */
  BatteryTaskHandle = osThreadNew(battery, NULL, &BatteryTask_attributes);

  /* creation of LEDTask */
  LEDTaskHandle = osThreadNew(led, NULL, &LEDTask_attributes);

  /* creation of UltraTask */
  UltraTaskHandle = osThreadNew(ultra, NULL, &UltraTask_attributes);

  /* creation of MotorTask */
  MotorTaskHandle = osThreadNew(motor, NULL, &MotorTask_attributes);

  /* creation of EncoderTask */
  EncoderTaskHandle = osThreadNew(encoder, NULL, &EncoderTask_attributes);

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
  * @brief TIM4 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM4_Init(void)
{

  /* USER CODE BEGIN TIM4_Init 0 */

  /* USER CODE END TIM4_Init 0 */

  TIM_MasterConfigTypeDef sMasterConfig = {0};
  TIM_IC_InitTypeDef sConfigIC = {0};

  /* USER CODE BEGIN TIM4_Init 1 */

  /* USER CODE END TIM4_Init 1 */
  htim4.Instance = TIM4;
  htim4.Init.Prescaler = 15;
  htim4.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim4.Init.Period = 65535;
  htim4.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim4.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  if (HAL_TIM_IC_Init(&htim4) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim4, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sConfigIC.ICPolarity = TIM_INPUTCHANNELPOLARITY_RISING;
  sConfigIC.ICSelection = TIM_ICSELECTION_DIRECTTI;
  sConfigIC.ICPrescaler = TIM_ICPSC_DIV1;
  sConfigIC.ICFilter = 0;
  if (HAL_TIM_IC_ConfigChannel(&htim4, &sConfigIC, TIM_CHANNEL_3) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM4_Init 2 */

  /* USER CODE END TIM4_Init 2 */

}

/**
  * @brief TIM5 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM5_Init(void)
{

  /* USER CODE BEGIN TIM5_Init 0 */

  /* USER CODE END TIM5_Init 0 */

  TIM_ClockConfigTypeDef sClockSourceConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};

  /* USER CODE BEGIN TIM5_Init 1 */

  /* USER CODE END TIM5_Init 1 */
  htim5.Instance = TIM5;
  htim5.Init.Prescaler = 15;
  htim5.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim5.Init.Period = 4294967295;
  htim5.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim5.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  if (HAL_TIM_Base_Init(&htim5) != HAL_OK)
  {
    Error_Handler();
  }
  sClockSourceConfig.ClockSource = TIM_CLOCKSOURCE_INTERNAL;
  if (HAL_TIM_ConfigClockSource(&htim5, &sClockSourceConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim5, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM5_Init 2 */

  /* USER CODE END TIM5_Init 2 */

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

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(ULTRA_TRIG_GPIO_Port, ULTRA_TRIG_Pin, GPIO_PIN_RESET);

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

  /*Configure GPIO pin : ULTRA_TRIG_Pin */
  GPIO_InitStruct.Pin = ULTRA_TRIG_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(ULTRA_TRIG_GPIO_Port, &GPIO_InitStruct);

  /*Configure GPIO pin : ENABLE_Pin */
  GPIO_InitStruct.Pin = ENABLE_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_INPUT;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  HAL_GPIO_Init(ENABLE_GPIO_Port, &GPIO_InitStruct);

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

void HAL_TIM_IC_CaptureCallback(TIM_HandleTypeDef *htim)
{
  if (htim->Instance == TIM4 && htim->Channel == HAL_TIM_ACTIVE_CHANNEL_3)
  {
    if (ultraCapture == 0)
    {
      __HAL_TIM_SET_COUNTER(htim, 0);
      __HAL_TIM_SET_CAPTUREPOLARITY(htim, TIM_CHANNEL_3, TIM_INPUTCHANNELPOLARITY_FALLING);

      ultraCapture = 1;
    }
    else
    {
      uint32_t tempVal = HAL_TIM_ReadCapturedValue(htim, TIM_CHANNEL_3);

      if (ultraCounter > 4)
    	valShift(ultraVal, 5);

      ultraVal[ultraCounter == 5 ? 4 : ultraCounter++] = tempVal;
      __HAL_TIM_SET_CAPTUREPOLARITY(htim, TIM_CHANNEL_3, TIM_INPUTCHANNELPOLARITY_RISING);
      __HAL_TIM_DISABLE_IT(&htim4, TIM_IT_CC3);

      ultraCapture = 0;
    }
  }
}

void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart)
{
  UNUSED(huart);
}

float avgVal(uint32_t *val, uint32_t size, float grad, float inter)
{
  float totalVal = 0.0;

  for (int counter = 0; counter < size; ++counter)
	totalVal += val[counter];

  return size == 0 ? 0 : totalVal / size * grad + inter;
}

uint8_t bufferFilled(uint8_t *buffer, uint32_t size)
{
  for (int counter = 0; counter < size; ++counter)
	if (buffer[counter] == 0)
	  return 0;

  return 1;
}

void microDelay(uint32_t us)
{
  __HAL_TIM_SET_COUNTER(&htim5, 0);
  while (__HAL_TIM_GET_COUNTER(&htim5) < us);
}

void valShift(uint32_t *val, uint32_t size)
{
  for (int counter = 0; counter < size - 1; ++counter)
	val[counter] = val[counter + 1];
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

	encoderTarget = 130;

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

	encoderTarget = 130;

	break;

  case 2: //Outside Lab
	encoderGrad = 0.208496572267945;
	encoderInt = 1.08776618891631;

	kdb = 0;
	kdf = 2;
	kib = 0;
	kif = 1;
	kpb = 0;
	kpf = 10;

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

	encoderTarget = 130;
  }
}

void driveRobot(uint8_t *cmd)
{
  if (startDriving == 0)
  {
	startDriving = 1;

	distA = 0.0;
	distB = 0.0;
	tempA = 0.0;
	tempB = 0.0;

	if (angleCmd == 0)
	{
	  char distStr[5];
	  strncpy(distStr, (char*)(cmd + 6), 4);
	  robotDist = (float)atoi(distStr);

	  driveCmd = cmd[4];

	  distBuffer = distOffset;

	  errorA = 0;
	  errorB = 0;
	  prevEncoderA = 0;
	  prevEncoderB = 0;
	  iTermA = 0.0;
	  iTermB = 0.0;

	  startPID = HAL_GPIO_ReadPin(ENABLE_GPIO_Port, ENABLE_Pin);

	  motorAVal = startPID == 1 ? 0 : driveMotorPWM;
	  motorBVal = motorAVal;
	}
	else
	{
	  distBuffer = turnOffset;

	  motorAVal = turnMotorPWM;
	  motorBVal = turnMotorPWM;
	}
  }
  else if (distA >= robotDist - distBuffer && distB >= robotDist - distBuffer)
	stopRobot();
}

void pid(uint32_t dur)
{
  if (startPID == 1)
  {
	float kd, ki, kp;

	kd = (driveCmd == 'F' ? kdf : kdb) * 1000 / dur;
	ki = (driveCmd == 'F' ? kif : kib) * dur / 1000;
	kp = driveCmd == 'F' ? kpf : kpb;

	errorA = encoderTarget - encoderAVal;
	errorB = encoderTarget - encoderBVal;

	iTermA += ki * errorA;
	iTermB += ki * errorB;

	if (iTermA < 0)
	  iTermA = 0;
	else if (iTermA > maxMotorPWM)
	  iTermA = maxMotorPWM;

	if (iTermB < 0)
	  iTermB = 0;
	else if (iTermB > maxMotorPWM)
	  iTermB = maxMotorPWM;

	motorAVal = (int)(kp * errorA - kd * (encoderAVal - prevEncoderA) + iTermA);
	motorBVal = (int)(kp * errorB - kd * (encoderBVal - prevEncoderB) + iTermB);

	uint8_t strBuffer[20];
	sprintf((char*)strBuffer, "%-4d %-4d", motorAVal, motorBVal);
	HAL_UART_Transmit(&huart3, strBuffer, 20, 0xFFFF);

	prevEncoderA = encoderAVal;
	prevEncoderB = encoderBVal;
  }
}

void stopRobot()
{
  startPID = 0;

  motorAVal = 0;
  motorBVal = 0;
  osDelay(stoppingDelay);

  servoVal = servoCenter;
  osDelay(turningDelay);

  startDriving = 0;
  angleCmd = 0;
  driveCmd = 0;
  cmdState = 2;
}

void turnRobot(uint8_t *cmd)
{
  if (angleCmd == 0)
  {
    char angleStr[4];
    strncpy(angleStr, (char*)(cmd + 7), 3);
    robotAngle = atoi(angleStr);

    angleCmd = cmd[4];
    driveCmd = cmd[5];

    if (angleCmd == 'L' && driveCmd == 'B')
      robotDist = lbGrad * robotAngle + lbInt;
    else if (angleCmd == 'L' && driveCmd == 'F')
	  robotDist = lfGrad * robotAngle + lfInt;
    else if (angleCmd == 'R' && driveCmd == 'B')
  	  robotDist = rbGrad * robotAngle + rbInt;
    else if (angleCmd == 'R' && driveCmd == 'F')
	  robotDist = rfGrad * robotAngle + rfInt;

    servoVal = angleCmd == 'L' ? servoLeft : servoRight;
    osDelay(turningDelay);
  }
  else
	driveRobot(cmd);
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
  uint8_t strBuffer[20];
  /* Infinite loop */
  for(;;)
  {
	sprintf((char*)strBuffer, "%d", profile);
	OLED_ShowString(0, 0, strBuffer);

	sprintf((char*)strBuffer, "%d", (int)HAL_GPIO_ReadPin(ENABLE_GPIO_Port, ENABLE_Pin));
	OLED_ShowString(0, 10, strBuffer);

	sprintf((char*)strBuffer, "          ");

	if (actionCounter <= fillCounter && bufferFilled(cmds[actionCounter], 10) == 1)
      strncpy((char*)strBuffer, (char*)cmds[actionCounter], 20);

	OLED_ShowString(10, 0, strBuffer);

	//sprintf((char*)strBuffer, "DistA: %-5d", (int)(distA + 0.5));
    //sprintf((char*)strBuffer, "MotorA: %-5d", (int)motorAVal);
	sprintf((char*)strBuffer, "EncA: %-5d", (int)encoderAVal);
	//sprintf((char*)strBuffer, "IR: %-5d", (int)(avgVal(irVal, irCounter, irGrad, irInt) + 0.5));
	//sprintf((char*)strBuffer, "Ultra: %-5d", (int)(ultraDist + 0.5));
	//sprintf((char*)strBuffer, "Tick1: %-5d", (int)tick1);
	//sprintf((char*)strBuffer, "TempA: %-5d", (int)tempA);
	OLED_ShowString(10, 10, strBuffer);

	//sprintf((char*)strBuffer, "DistB: %-5d", (int)(distB + 0.5));
	//sprintf((char*)strBuffer, "MotorB: %-5d", (int)motorBVal);
	sprintf((char*)strBuffer, "EncB: %-5d", (int)encoderBVal);
	//sprintf((char*)strBuffer, "Tick2: %-5d", (int)tick2);
	//sprintf((char*)strBuffer, "TempB: %-5d", (int)tempB);
	//sprintf((char*)strBuffer, "Ultra: %-5d", (int)(avgVal(ultraVal, ultraCounter, ultraGrad, ultraInt) + 0.5));
	OLED_ShowString(10, 20, strBuffer);

	//sprintf((char*)strBuffer, "Action: %-5d", (int)actionCounter);
	sprintf((char*)strBuffer, "DistA: %-5d", (int)(distA + 0.5));
	//sprintf((char*)strBuffer, "TempA: %-5d", (int)tempA);
	//sprintf((char*)strBuffer, "TempA: %-5d", (int)ultraCount1);
	OLED_ShowString(10, 30, strBuffer);

	//sprintf((char*)strBuffer, "Fill: %-5d", (int)fillCounter);
	sprintf((char*)strBuffer, "DistB: %-5d", (int)(distB + 0.5));
	//sprintf((char*)strBuffer, "TempB: %-5d", (int)tempB);
	//sprintf((char*)strBuffer, "TempA: %-5d", (int)ultraCount2);
	OLED_ShowString(10, 40, strBuffer);

	OLED_ShowString(10, 50, batteryState);

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
  /* Infinite loop */
  for(;;)
  {
	if (irCounter > 4)
	  valShift(irVal, 5);

	HAL_ADC_Start(&hadc1);
	HAL_ADC_PollForConversion(&hadc1, 0xFFFF);
	irVal[irCounter == 5 ? 4 : irCounter++] = HAL_ADC_GetValue(&hadc1);

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

	if (bufferFilled(rxBuffer, 10) == 1)
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
      //sprintf((char*)txBuffer, cmdType == 'C' ? "Done C" : "Done %d", atoi(strCounter));
      //HAL_UART_Transmit(&huart3, txBuffer, 20, 0xFFFF);

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

	__HAL_TIM_SetCompare(&htim1, TIM_CHANNEL_4, angleCmd == 0 ? servoCenter : servoVal);

	osDelay(servoDelay);
  }
  /* USER CODE END servo */
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
  /* Infinite loop */
  for(;;)
  {
	if (cmdState == 1 && actionCounter <= fillCounter)
	{
	  switch (cmds[actionCounter][4])
	  {
	  case 'F':
	  case 'B':
		driveRobot(cmds[actionCounter]);
		break;

	  case 'L':
	  case 'R':
		turnRobot(cmds[actionCounter]);
		break;

	  case 'C':
		cmdState = 2;
		break;

	  default:
		++actionCounter;
	  }
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
  float avgBattery;
  /* Infinite loop */
  for(;;)
  {
	if (batteryCounter > 4)
	  valShift(batteryVal, 5);

	HAL_ADC_Start(&hadc2);
	HAL_ADC_PollForConversion(&hadc2, 0xFFFF);
	batteryVal[batteryCounter == 5 ? 4 : batteryCounter++] = HAL_ADC_GetValue(&hadc2);

	avgBattery = avgVal(batteryVal, batteryCounter, 1.0, 0.0);

	if (avgBattery > 1360)
	  sprintf((char*)batteryState, "Batt: Full");
	else if (avgBattery > 1310)
	  sprintf((char*)batteryState, "Batt: Norm");
	else if (avgBattery > 1000)
	  sprintf((char*)batteryState, "Batt: Chrg");
	else
	  sprintf((char*)batteryState, "Batt: Off");


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

/* USER CODE BEGIN Header_ultra */
/**
* @brief Function implementing the UltraTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_ultra */
void ultra(void *argument)
{
  /* USER CODE BEGIN ultra */
  HAL_TIM_IC_Start_IT(&htim4, TIM_CHANNEL_3);
  HAL_TIM_Base_Start(&htim5);
  /* Infinite loop */
  for(;;)
  {
	HAL_GPIO_WritePin(ULTRA_TRIG_GPIO_Port, ULTRA_TRIG_Pin, GPIO_PIN_SET);
	microDelay(10);
	HAL_GPIO_WritePin(ULTRA_TRIG_GPIO_Port, ULTRA_TRIG_Pin, GPIO_PIN_RESET);

	__HAL_TIM_ENABLE_IT(&htim4, TIM_IT_CC3);

	osDelay(ultraDelay);
  }
  /* USER CODE END ultra */
}

/* USER CODE BEGIN Header_motor */
/**
* @brief Function implementing the MotorTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_motor */
void motor(void *argument)
{
  /* USER CODE BEGIN motor */
  HAL_TIM_PWM_Start_IT(&htim8, TIM_CHANNEL_1);
  HAL_TIM_PWM_Start_IT(&htim8, TIM_CHANNEL_2);
  /* Infinite loop */
  for(;;)
  {
	if (motorAVal < 0)
	  motorAVal = 0;
	else if (motorAVal > maxMotorPWM)
	  motorAVal = maxMotorPWM;

	if (motorBVal < 0)
	  motorBVal = 0;
	else if (motorBVal > maxMotorPWM)
	  motorBVal = maxMotorPWM;

	HAL_GPIO_WritePin(GPIOA, AIN1_Pin, driveCmd == 'F' ? GPIO_PIN_SET : GPIO_PIN_RESET);
	HAL_GPIO_WritePin(GPIOA, AIN2_Pin, driveCmd == 'F' ? GPIO_PIN_RESET : GPIO_PIN_SET);
	HAL_GPIO_WritePin(GPIOA, BIN1_Pin, driveCmd == 'F' ? GPIO_PIN_SET : GPIO_PIN_RESET);
	HAL_GPIO_WritePin(GPIOA, BIN2_Pin, driveCmd == 'F' ? GPIO_PIN_RESET : GPIO_PIN_SET);

	__HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, driveCmd == 0 ? 0 : (uint32_t)motorAVal);
	__HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, driveCmd == 0 ? 0 : (uint32_t)motorBVal);

	osDelay(motorDelay);
  }
  /* USER CODE END motor */
}

/* USER CODE BEGIN Header_encoder */
/**
* @brief Function implementing the EncoderTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_encoder */
void encoder(void *argument)
{
  /* USER CODE BEGIN encoder */
  uint32_t cnt1A, cnt1B, cnt2A, cnt2B, diff1A, diff1B, diff2A, diff2B, dur, tick;

  HAL_TIM_Encoder_Start(&htim2, TIM_CHANNEL_ALL);
  HAL_TIM_Encoder_Start(&htim3, TIM_CHANNEL_ALL);

  cnt1A = __HAL_TIM_GET_COUNTER(&htim2);
  cnt1B = __HAL_TIM_GET_COUNTER(&htim3);

  tick = HAL_GetTick();
  /* Infinite loop */
  for(;;)
  {
    dur = HAL_GetTick() - tick;

	if (dur > encoderDelay)
	{
	  cnt2A = __HAL_TIM_GET_COUNTER(&htim2);
	  cnt2B = __HAL_TIM_GET_COUNTER(&htim3);

	  diff1A = abs(cnt1A - cnt2A);
	  diff1B = abs(cnt1B - cnt2B);
	  diff2A = abs(65535 - (__HAL_TIM_IS_TIM_COUNTING_DOWN(&htim2) ? cnt2A - cnt1A : cnt1A - cnt2A));
	  diff2B = abs(65535 - (__HAL_TIM_IS_TIM_COUNTING_DOWN(&htim3) ? cnt2B - cnt1B : cnt1B - cnt2B));

	  encoderAVal = diff1A < diff2A ? diff1A : diff2A;
	  encoderBVal = diff1B < diff2B ? diff1B : diff2B;

	  if (startDriving == 1)
	  {
		tempA += pi * radius * encoderAVal / 165;
		tempB += pi * radius * encoderBVal / 165;

		if (distA == 0.0)
		{
		  distA += encoderInt;
		  distB += encoderInt;
		}

		distA += encoderGrad * pi * radius * encoderAVal / 165;
		distB += encoderGrad * pi * radius * encoderBVal / 165;
	  }

	  pid(dur);

	  cnt1A = __HAL_TIM_GET_COUNTER(&htim2);
	  cnt1B = __HAL_TIM_GET_COUNTER(&htim3);

	  tick = HAL_GetTick();
	}
  }
  /* USER CODE END encoder */
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

